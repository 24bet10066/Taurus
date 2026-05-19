package com.serviceos.payment.service;

import com.serviceos.payment.dto.request.CashPaymentRequest;
import com.serviceos.payment.dto.request.VerifyPaymentRequest;
import com.serviceos.payment.dto.response.PaymentStatusResponse;
import com.serviceos.payment.entity.PaymentRecord;
import com.serviceos.payment.feign.JobServiceClient;
import com.serviceos.payment.repository.PaymentRecordRepository;
import com.serviceos.shared.dto.ApiResponse;
import com.serviceos.shared.enums.JobStatus;
import com.serviceos.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceTest {

    @Mock PaymentRecordRepository paymentRepository;
    @Mock RazorpayService razorpayService;
    @Mock PaymentEventPublisher eventPublisher;
    @Mock JobServiceClient jobServiceClient;

    @InjectMocks PaymentService paymentService;

    private final UUID jobId = UUID.randomUUID();
    private final UUID collectedBy = UUID.randomUUID();

    private JobServiceClient.JobStatusDTO completedJobStatus;
    private JobServiceClient.JobStatusDTO requestedJobStatus;

    @BeforeEach
    void setUp() {
        completedJobStatus = new JobServiceClient.JobStatusDTO(
                jobId, JobStatus.COMPLETED, "PENDING",
                new BigDecimal("1200.00"), "9876543210");
        requestedJobStatus = new JobServiceClient.JobStatusDTO(
                jobId, JobStatus.REQUESTED, "PENDING",
                null, "9876543210");

        when(paymentRepository.save(any())).thenAnswer(inv -> {
            PaymentRecord r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            if (r.getCreatedAt() == null) r.setCreatedAt(Instant.now());
            return r;
        });
        doNothing().when(eventPublisher).publishPaymentReceived(any());
        when(jobServiceClient.markPaymentCollected(any(), any(), any()))
                .thenReturn(ApiResponse.ok("updated"));
    }

    @Test
    void collectCash_completedJob_recordsPayment() {
        when(jobServiceClient.getJobStatus(jobId))
                .thenReturn(ApiResponse.ok(completedJobStatus));
        when(paymentRepository.findByJobIdOrderByCreatedAtDesc(jobId)).thenReturn(List.of());

        PaymentStatusResponse response = paymentService.collectCash(
                new CashPaymentRequest(jobId, new BigDecimal("1200.00"), collectedBy, "Cash collected"),
                collectedBy);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.paymentMethod()).isEqualTo("CASH");
        assertThat(response.amount()).isEqualByComparingTo("1200.00");
        verify(paymentRepository).save(any());
        verify(eventPublisher).publishPaymentReceived(any());
    }

    @Test
    void collectCash_jobNotCompleted_throws() {
        when(jobServiceClient.getJobStatus(jobId))
                .thenReturn(ApiResponse.ok(requestedJobStatus));

        assertThatThrownBy(() -> paymentService.collectCash(
                new CashPaymentRequest(jobId, new BigDecimal("500.00"), collectedBy, null),
                collectedBy))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void collectCash_alreadyCollected_throws() {
        var collectedStatus = new JobServiceClient.JobStatusDTO(
                jobId, JobStatus.COMPLETED, "COLLECTED",
                new BigDecimal("1200.00"), "9876543210");
        when(jobServiceClient.getJobStatus(jobId)).thenReturn(ApiResponse.ok(collectedStatus));

        assertThatThrownBy(() -> paymentService.collectCash(
                new CashPaymentRequest(jobId, new BigDecimal("1200.00"), collectedBy, null),
                collectedBy))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already");
    }

    @Test
    void createPendingRecord_newJob_createsPendingEntry() {
        when(paymentRepository.findByJobIdOrderByCreatedAtDesc(jobId)).thenReturn(List.of());

        paymentService.createPendingRecord(jobId, new BigDecimal("900.00"), "9876543210");

        verify(paymentRepository).save(argThat(r ->
                "PENDING".equals(r.getStatus()) && r.getJobId().equals(jobId)));
    }

    @Test
    void createPendingRecord_alreadyExists_skips() {
        PaymentRecord existing = new PaymentRecord();
        existing.setJobId(jobId);
        when(paymentRepository.findByJobIdOrderByCreatedAtDesc(jobId)).thenReturn(List.of(existing));

        paymentService.createPendingRecord(jobId, new BigDecimal("900.00"), null);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void verifyAndCapture_invalidSignature_throws() {
        when(razorpayService.verifySignature(any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> paymentService.verifyAndCapture(
                new VerifyPaymentRequest("pay_abc", "order_abc", "bad_sig", jobId)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void verifyAndCapture_validSignature_capturesPayment() {
        when(razorpayService.verifySignature(any(), any(), any())).thenReturn(true);
        when(jobServiceClient.getJobStatus(jobId)).thenReturn(ApiResponse.ok(completedJobStatus));
        when(paymentRepository.findByJobId(jobId)).thenReturn(Optional.empty());

        PaymentStatusResponse response = paymentService.verifyAndCapture(
                new VerifyPaymentRequest("pay_abc123", "order_xyz", "valid_sig", jobId));

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.razorpayPaymentId()).isEqualTo("pay_abc123");
        verify(eventPublisher).publishPaymentReceived(any());
    }

    @Test
    void getByJobId_returnsAllRecords() {
        PaymentRecord r = new PaymentRecord();
        r.setId(UUID.randomUUID()); r.setJobId(jobId);
        r.setAmount(new BigDecimal("500")); r.setPaymentMethod("CASH");
        r.setStatus("COMPLETED"); r.setCreatedAt(Instant.now());
        when(paymentRepository.findByJobIdOrderByCreatedAtDesc(jobId)).thenReturn(List.of(r));

        List<PaymentStatusResponse> result = paymentService.getByJobId(jobId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo("COMPLETED");
    }
}
