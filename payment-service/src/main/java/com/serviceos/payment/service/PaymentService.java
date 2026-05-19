package com.serviceos.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviceos.payment.dto.request.CashPaymentRequest;
import com.serviceos.payment.dto.request.CreateOrderRequest;
import com.serviceos.payment.dto.request.VerifyPaymentRequest;
import com.serviceos.payment.dto.response.CreateOrderResponse;
import com.serviceos.payment.dto.response.PaymentStatusResponse;
import com.serviceos.payment.entity.PaymentRecord;
import com.serviceos.payment.feign.JobServiceClient;
import com.serviceos.payment.repository.PaymentRecordRepository;
import com.serviceos.shared.enums.JobStatus;
import com.serviceos.shared.exception.BusinessRuleViolationException;
import com.serviceos.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PaymentRecordRepository paymentRepository;
    private final RazorpayService razorpayService;
    private final PaymentEventPublisher eventPublisher;
    private final JobServiceClient jobServiceClient;

    @Value("${serviceos.razorpay.key-id:}")
    private String razorpayKeyId;

    public PaymentService(PaymentRecordRepository paymentRepository,
                          RazorpayService razorpayService,
                          PaymentEventPublisher eventPublisher,
                          JobServiceClient jobServiceClient) {
        this.paymentRepository = paymentRepository;
        this.razorpayService   = razorpayService;
        this.eventPublisher    = eventPublisher;
        this.jobServiceClient  = jobServiceClient;
    }

    // -------------------------------------------------------------------------
    // Cash payment (most common flow)
    // -------------------------------------------------------------------------

    @Transactional
    public PaymentStatusResponse collectCash(CashPaymentRequest req, UUID collectedBy) {
        // Validate job status via job-service
        var jobStatus = getJobStatus(req.jobId());
        if (jobStatus.status() != JobStatus.COMPLETED) {
            throw new BusinessRuleViolationException("JOB_NOT_COMPLETED",
                    "Job must be COMPLETED before collecting payment");
        }
        if (!"PENDING".equals(jobStatus.paymentStatus())) {
            throw new BusinessRuleViolationException("PAYMENT_ALREADY_COLLECTED",
                    "Payment has already been recorded for this job");
        }

        PaymentRecord record = new PaymentRecord();
        record.setJobId(req.jobId());
        record.setAmount(req.amount());
        record.setPaymentMethod("CASH");
        record.setStatus("COMPLETED");
        record.setCollectedBy(collectedBy != null ? collectedBy : req.collectedBy());
        record.setCustomerPhone(jobStatus.customerPhone());
        record.setNotes(req.notes());
        record.setCompletedAt(Instant.now());
        record = paymentRepository.save(record);

        notifyJobService(req.jobId(), "COLLECTED", "CASH");
        eventPublisher.publishPaymentReceived(record);

        log.info("Cash payment collected: paymentId={} jobId={} amount={}",
                record.getId(), req.jobId(), req.amount());
        return toResponse(record);
    }

    // -------------------------------------------------------------------------
    // Razorpay online flow
    // -------------------------------------------------------------------------

    @Transactional
    public CreateOrderResponse createRazorpayOrder(CreateOrderRequest req) {
        var jobStatus = getJobStatus(req.jobId());
        if (jobStatus.status() != JobStatus.COMPLETED) {
            throw new BusinessRuleViolationException("JOB_NOT_COMPLETED",
                    "Job must be COMPLETED before initiating online payment");
        }
        var amount = jobStatus.actualCharge() != null
                ? jobStatus.actualCharge()
                : java.math.BigDecimal.ZERO;

        var orderResult = razorpayService.createOrder(req.jobId(), amount);

        // Create a PENDING record linked to the Razorpay order
        PaymentRecord pending = new PaymentRecord();
        pending.setJobId(req.jobId());
        pending.setAmount(amount);
        pending.setPaymentMethod("RAZORPAY");
        pending.setStatus("PENDING");
        pending.setRazorpayOrderId(orderResult.orderId());
        pending.setCustomerPhone(jobStatus.customerPhone());
        paymentRepository.save(pending);

        return new CreateOrderResponse(orderResult.orderId(), amount, "INR",
                req.jobId(), razorpayKeyId);
    }

    @Transactional
    public PaymentStatusResponse verifyAndCapture(VerifyPaymentRequest req) {
        if (!razorpayService.verifySignature(req.razorpayOrderId(),
                req.razorpayPaymentId(), req.razorpaySignature())) {
            throw new BusinessRuleViolationException("INVALID_SIGNATURE",
                    "Razorpay payment signature verification failed");
        }

        // Find the pending record for this order or create a new one
        PaymentRecord record = paymentRepository.findByJobId(req.jobId())
                .filter(r -> req.razorpayOrderId().equals(r.getRazorpayOrderId()))
                .orElseGet(PaymentRecord::new);

        record.setJobId(req.jobId());
        record.setPaymentMethod("RAZORPAY");
        record.setRazorpayOrderId(req.razorpayOrderId());
        record.setRazorpayPaymentId(req.razorpayPaymentId());
        record.setStatus("COMPLETED");
        record.setCompletedAt(Instant.now());

        if (record.getAmount() == null) {
            var jobStatus = getJobStatus(req.jobId());
            record.setAmount(jobStatus.actualCharge() != null
                    ? jobStatus.actualCharge() : java.math.BigDecimal.ZERO);
            record.setCustomerPhone(jobStatus.customerPhone());
        }

        record = paymentRepository.save(record);

        notifyJobService(req.jobId(), "COLLECTED", "RAZORPAY");
        eventPublisher.publishPaymentReceived(record);

        log.info("Razorpay payment verified: paymentId={} jobId={} rzpPaymentId={}",
                record.getId(), req.jobId(), req.razorpayPaymentId());
        return toResponse(record);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PaymentStatusResponse> getByJobId(UUID jobId) {
        return paymentRepository.findByJobIdOrderByCreatedAtDesc(jobId)
                .stream().map(this::toResponse).toList();
    }

    // -------------------------------------------------------------------------
    // Internal: create PENDING record on job.completed (from Kafka consumer)
    // -------------------------------------------------------------------------

    @Transactional
    public void createPendingRecord(UUID jobId, java.math.BigDecimal amount, String customerPhone) {
        boolean exists = !paymentRepository.findByJobIdOrderByCreatedAtDesc(jobId).isEmpty();
        if (exists) return; // already has a record
        PaymentRecord record = new PaymentRecord();
        record.setJobId(jobId);
        record.setAmount(amount != null ? amount : java.math.BigDecimal.ZERO);
        record.setPaymentMethod("PENDING");
        record.setStatus("PENDING");
        record.setCustomerPhone(customerPhone);
        paymentRepository.save(record);
        log.debug("Created PENDING payment record for jobId={}", jobId);
    }

    // -------------------------------------------------------------------------
    // Razorpay webhook
    // -------------------------------------------------------------------------

    @Transactional
    public void handleRazorpayWebhook(String rawBody) {
        try {
            JsonNode root    = MAPPER.readTree(rawBody);
            String eventType = root.path("event").asText();
            JsonNode entity  = root.path("payload").path("payment").path("entity");
            String orderId   = entity.path("order_id").asText(null);
            String paymentId = entity.path("id").asText(null);

            switch (eventType) {
                case "payment.captured" -> {
                    if (orderId == null) return;
                    paymentRepository.findByRazorpayOrderId(orderId).ifPresent(record -> {
                        if ("COMPLETED".equals(record.getStatus())) return;
                        record.setStatus("COMPLETED");
                        record.setRazorpayPaymentId(paymentId);
                        record.setCompletedAt(Instant.now());
                        paymentRepository.save(record);
                        notifyJobService(record.getJobId(), "COLLECTED", "RAZORPAY");
                        eventPublisher.publishPaymentReceived(record);
                        log.info("Webhook: payment captured orderId={} paymentId={}", orderId, paymentId);
                    });
                }
                case "payment.failed" -> {
                    if (orderId == null) return;
                    paymentRepository.findByRazorpayOrderId(orderId).ifPresent(record -> {
                        record.setStatus("FAILED");
                        paymentRepository.save(record);
                        log.info("Webhook: payment failed orderId={}", orderId);
                    });
                }
                default -> log.debug("Webhook: ignoring event type={}", eventType);
            }
        } catch (Exception ex) {
            log.error("Webhook processing error: {}", ex.getMessage(), ex);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private JobServiceClient.JobStatusDTO getJobStatus(UUID jobId) {
        try {
            var response = jobServiceClient.getJobStatus(jobId);
            if (response == null || response.data() == null) {
                throw new ResourceNotFoundException("Job", jobId);
            }
            return response.data();
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Job status lookup failed for jobId={}: {}", jobId, ex.getMessage());
            throw new BusinessRuleViolationException("JOB_LOOKUP_FAILED",
                    "Could not verify job status: " + ex.getMessage());
        }
    }

    private void notifyJobService(UUID jobId, String status, String method) {
        try {
            jobServiceClient.markPaymentCollected(jobId, status, method);
        } catch (Exception ex) {
            log.warn("Failed to update job payment status for jobId={}: {}", jobId, ex.getMessage());
        }
    }

    PaymentStatusResponse toResponse(PaymentRecord r) {
        return new PaymentStatusResponse(
                r.getId(), r.getJobId(), r.getAmount(), r.getPaymentMethod(),
                r.getStatus(), r.getRazorpayOrderId(), r.getRazorpayPaymentId(),
                r.getCreatedAt(), r.getCompletedAt()
        );
    }
}
