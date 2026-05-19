package com.serviceos.job.service;

import com.serviceos.job.dto.request.*;
import com.serviceos.job.dto.response.JobResponse;
import com.serviceos.job.dto.response.JobTechnicianResponse;
import com.serviceos.job.dto.response.PublicBookingResponse;
import com.serviceos.job.entity.Job;
import com.serviceos.job.entity.JobPartUsed;
import com.serviceos.job.entity.JobTechnician;
import com.serviceos.job.entity.JobTechnicianId;
import com.serviceos.job.feign.AvailableTechnicianDTO;
import com.serviceos.job.feign.TechnicianClient;
import com.serviceos.job.repository.*;
import com.serviceos.job.security.AuthenticatedUser;
import com.serviceos.shared.dto.PageResponse;
import com.serviceos.shared.enums.JobStatus;
import com.serviceos.shared.enums.Role;
import com.serviceos.shared.exception.BusinessRuleViolationException;
import com.serviceos.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobRepository;
    private final JobTechnicianRepository jobTechnicianRepository;
    private final JobPartUsedRepository jobPartUsedRepository;
    private final JobFSM jobFSM;
    private final AssignmentEngine assignmentEngine;
    private final JobEventPublisher eventPublisher;
    private final PublicBookingRateLimiter rateLimiter;
    private final TechnicianClient technicianClient;
    private final ChargeCalculationService chargeCalc;

    public JobService(JobRepository jobRepository,
                      JobTechnicianRepository jobTechnicianRepository,
                      JobPartUsedRepository jobPartUsedRepository,
                      JobFSM jobFSM,
                      AssignmentEngine assignmentEngine,
                      JobEventPublisher eventPublisher,
                      PublicBookingRateLimiter rateLimiter,
                      TechnicianClient technicianClient,
                      ChargeCalculationService chargeCalc) {
        this.jobRepository = jobRepository;
        this.jobTechnicianRepository = jobTechnicianRepository;
        this.jobPartUsedRepository = jobPartUsedRepository;
        this.jobFSM = jobFSM;
        this.assignmentEngine = assignmentEngine;
        this.eventPublisher = eventPublisher;
        this.rateLimiter = rateLimiter;
        this.technicianClient = technicianClient;
        this.chargeCalc = chargeCalc;
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Transactional
    public JobResponse createJob(CreateJobRequest req, UUID createdBy) {
        Job job = new Job();
        job.setCustomerId(req.customerId());
        job.setCustomerName(req.customerName());
        job.setCustomerPhone(req.customerPhone());
        job.setApplianceId(req.applianceId());
        job.setApplianceType(req.applianceType());
        job.setBrand(req.brand());
        job.setSource(req.source());
        job.setIssueDescription(req.issueDescription());
        job.setPriority(req.priority() != null ? req.priority() : "NORMAL");
        job.setArea(req.area());
        job.setCustomerNotes(req.customerNotes());
        job.setStatus(JobStatus.REQUESTED);
        // Apply dynamic charges from business config (base + travel surcharge)
        chargeCalc.applyCharges(job);
        // Allow admin to override estimated charge; final_charge is always auto-computed
        if (req.estimatedCharge() != null) job.setEstimatedCharge(req.estimatedCharge());
        job = jobRepository.save(job);

        eventPublisher.publishJobCreated(job);
        return toResponse(job);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<JobResponse> listJobs(JobStatus status, String area, LocalDate date,
                                              String customerPhone, UUID technicianId,
                                              int page, int size) {
        Instant from = null;
        Instant to = null;
        if (date != null) {
            ZoneId tz = ZoneId.of("Asia/Kolkata");
            from = date.atStartOfDay(tz).toInstant();
            to = date.plusDays(1).atStartOfDay(tz).toInstant();
        }
        // Normalise phone — strip everything except digits, keep last 10 (handles +91/space variations)
        String phone = null;
        if (customerPhone != null && !customerPhone.isBlank()) {
            String digits = customerPhone.replaceAll("\\D", "");
            if (digits.length() >= 10) phone = digits.substring(digits.length() - 10);
        }
        Page<Job> jobPage = jobRepository.findByFilters(
                status, area, from, to, phone, technicianId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<JobResponse> content = jobPage.getContent().stream().map(this::toResponse).toList();
        return PageResponse.of(content, page, size, jobPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(UUID jobId, AuthenticatedUser user) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
        if (user.role() == Role.TECHNICIAN_HIRED) {
            boolean assigned = jobTechnicianRepository.existsByJobIdAndTechnicianId(jobId, user.userId());
            if (!assigned) throw new AccessDeniedException("You are not assigned to this job");
        }
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getMyJobs(UUID technicianId) {
        return jobRepository.findByTechnicianId(technicianId)
                .stream().map(this::toResponse).toList();
    }

    // -------------------------------------------------------------------------
    // Assign
    // -------------------------------------------------------------------------

    @Transactional
    public JobResponse assignJob(UUID jobId, AssignJobRequest req, UUID assignedBy) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        if (job.getStatus().isTerminal()) {
            throw new BusinessRuleViolationException("JOB_TERMINAL",
                    "Cannot assign technicians to a terminal job");
        }

        // Remove existing assignments and decrement their counters
        List<JobTechnician> existing = jobTechnicianRepository.findByJobId(jobId);
        for (JobTechnician jt : existing) {
            safeDecrementActiveJobs(jt.getId().getTechnicianId());
        }
        jobTechnicianRepository.deleteByJobId(jobId);

        // Assign PRIMARY
        saveTechnician(jobId, req.primaryTechId(), "PRIMARY");
        safeIncrementActiveJobs(req.primaryTechId());

        // Assign optional ASSISTANT
        if (req.assistantTechId() != null) {
            saveTechnician(jobId, req.assistantTechId(), "ASSISTANT");
            safeIncrementActiveJobs(req.assistantTechId());
        }

        // Auto-transition REQUESTED → ASSIGNED
        if (job.getStatus() == JobStatus.REQUESTED) {
            jobFSM.transition(jobId, JobStatus.ASSIGNED, assignedBy, "Technician assigned");
        }

        return toResponse(jobRepository.findById(jobId).orElseThrow());
    }

    @Transactional
    public Optional<UUID> autoAssign(UUID jobId, UUID triggeredBy) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        if (job.getStatus().isTerminal()) {
            throw new BusinessRuleViolationException("JOB_TERMINAL",
                    "Cannot auto-assign a terminal job");
        }
        if (job.getApplianceType() == null) {
            throw new BusinessRuleViolationException("NO_APPLIANCE_TYPE",
                    "Job must have an appliance type for auto-assignment");
        }

        List<AvailableTechnicianDTO> candidates;
        try {
            candidates = technicianClient.getAvailableTechnicians(job.getApplianceType().name());
        } catch (Exception ex) {
            log.error("technician-service unavailable during auto-assign: {}", ex.getMessage());
            return Optional.empty();
        }

        Optional<UUID> bestTech = assignmentEngine.findBestTechnician(job.getApplianceType(), candidates);
        bestTech.ifPresent(techId -> {
            assignJob(jobId, new AssignJobRequest(techId, null), triggeredBy);
        });
        return bestTech;
    }

    // -------------------------------------------------------------------------
    // Parts
    // -------------------------------------------------------------------------

    @Transactional
    public JobResponse addPart(UUID jobId, AddPartRequest req, UUID addedBy) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        if (job.getStatus().isTerminal()) {
            throw new BusinessRuleViolationException("JOB_TERMINAL", "Cannot add parts to a terminal job");
        }

        JobPartUsed part = new JobPartUsed();
        part.setJobId(jobId);
        part.setPartId(req.partId());
        part.setPartName(req.partName());
        part.setQuantity(req.quantity());
        part.setUnitCost(req.unitCost());
        part.setSource(req.source() != null ? req.source() : "SHOP");
        jobPartUsedRepository.save(part);

        return toResponse(job);
    }

    // -------------------------------------------------------------------------
    // Payment
    // -------------------------------------------------------------------------

    @Transactional
    public JobResponse confirmPayment(UUID jobId, PaymentRequest req) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new BusinessRuleViolationException("PAYMENT_PREMATURE",
                    "Payment can only be confirmed on completed jobs");
        }

        job.setPaymentStatus("COLLECTED");
        job.setPaymentMethod(req.paymentMethod());
        job.setPaymentRef(req.paymentRef());
        job.setActualCharge(req.amount());
        return toResponse(jobRepository.save(job));
    }

    // -------------------------------------------------------------------------
    // Public booking
    // -------------------------------------------------------------------------

    @Transactional
    public PublicBookingResponse publicBooking(PublicBookingRequest req) {
        rateLimiter.checkAndIncrement(req.customerPhone());

        Job job = new Job();
        job.setCustomerPhone(req.customerPhone());
        job.setCustomerName(req.customerName());
        job.setApplianceType(req.applianceType());
        job.setBrand(req.brand());
        job.setArea(req.area());
        job.setIssueDescription(req.issueDescription());
        job.setCustomerNotes(req.customerNotes());
        // Marketing channel tracking — defaults to "WEB" when no ref param.
        // Length-clamp to 32 chars to avoid abuse via long query strings.
        String src = req.source();
        if (src == null || src.isBlank()) src = "WEB";
        if (src.length() > 32) src = src.substring(0, 32);
        job.setSource(src.toUpperCase());
        job.setStatus(JobStatus.REQUESTED);
        // customerId is unknown for anonymous booking — a placeholder zero UUID flags it for admin follow-up
        job.setCustomerId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        job = jobRepository.save(job);

        eventPublisher.publishJobCreated(job);
        log.info("Public booking created jobId={} phone={}", job.getId(), req.customerPhone());

        return new PublicBookingResponse(job.getId(), "Booking confirmed, we will contact you shortly");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void saveTechnician(UUID jobId, UUID techId, String role) {
        JobTechnicianId id = new JobTechnicianId(jobId, techId);
        jobTechnicianRepository.save(new JobTechnician(id, role));
    }

    private void safeIncrementActiveJobs(UUID techId) {
        try {
            technicianClient.updateActiveJobCount(techId, 1);
        } catch (Exception ex) {
            log.warn("Could not increment activeJobs for tech {}: {}", techId, ex.getMessage());
        }
    }

    private void safeDecrementActiveJobs(UUID techId) {
        try {
            technicianClient.updateActiveJobCount(techId, -1);
        } catch (Exception ex) {
            log.warn("Could not decrement activeJobs for tech {}: {}", techId, ex.getMessage());
        }
    }

    private JobResponse toResponse(Job job) {
        List<JobTechnicianResponse> techs = jobTechnicianRepository.findByJobId(job.getId())
                .stream()
                .map(jt -> new JobTechnicianResponse(
                        jt.getId().getTechnicianId(), jt.getRole(), jt.getAssignedAt()))
                .toList();

        return new JobResponse(
                job.getId(), job.getCustomerId(),
                job.getCustomerName(), job.getCustomerPhone(),
                job.getApplianceId(), job.getApplianceType(), job.getBrand(),
                job.getSource(), job.getIssueDescription(), job.getPriority(),
                job.getStatus(),
                job.getAssignedAt(), job.getCompletedAt(), job.getCancelledAt(),
                job.getEstimatedCharge(), job.getActualCharge(),
                job.getLaborCharge(), job.getPartsCharge(),
                job.getPaymentStatus(), job.getPaymentMethod(),
                job.getArea(), job.getCustomerNotes(), job.getTechnicianNotes(),
                techs, job.getCreatedAt(), job.getUpdatedAt()
        );
    }
}
