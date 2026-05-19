package com.serviceos.job.service;

import com.serviceos.job.entity.Job;
import com.serviceos.job.entity.JobStatusHistory;
import com.serviceos.job.repository.JobPartUsedRepository;
import com.serviceos.job.repository.JobRepository;
import com.serviceos.job.repository.JobStatusHistoryRepository;
import com.serviceos.job.repository.JobTechnicianRepository;
import com.serviceos.shared.enums.JobStatus;
import com.serviceos.shared.result.JobTransitionResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JobFSM {

    private final JobRepository jobRepository;
    private final JobStatusHistoryRepository historyRepository;
    private final JobPartUsedRepository partsRepository;
    private final JobTechnicianRepository technicianRepository;
    private final JobEventPublisher eventPublisher;

    public JobFSM(JobRepository jobRepository,
                  JobStatusHistoryRepository historyRepository,
                  JobPartUsedRepository partsRepository,
                  JobTechnicianRepository technicianRepository,
                  JobEventPublisher eventPublisher) {
        this.jobRepository = jobRepository;
        this.historyRepository = historyRepository;
        this.partsRepository = partsRepository;
        this.technicianRepository = technicianRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public JobTransitionResult transition(UUID jobId, JobStatus targetStatus, UUID changedBy, String reason) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return new JobTransitionResult.JobNotFound(jobId);

        JobStatus current = job.getStatus();

        if (current.isTerminal()) {
            return new JobTransitionResult.TerminalState(current);
        }

        if (!current.canTransitionTo(targetStatus)) {
            return new JobTransitionResult.InvalidTransition(
                    current, targetStatus,
                    "Transition " + current + " → " + targetStatus + " is not allowed"
            );
        }

        applyTransitionSideEffects(job, current, targetStatus);
        job.setStatus(targetStatus);
        jobRepository.save(job);

        recordHistory(job.getId(), current, targetStatus, changedBy, reason);
        eventPublisher.publishStatusChanged(job, current, targetStatus);
        if (targetStatus == JobStatus.COMPLETED) {
            eventPublisher.publishJobCompleted(job);
        }

        return new JobTransitionResult.Success(jobId, current, targetStatus, Instant.now());
    }

    private void applyTransitionSideEffects(Job job, JobStatus from, JobStatus to) {
        Instant now = Instant.now();
        switch (to) {
            case ASSIGNED    -> job.setAssignedAt(now);
            case COMPLETED   -> {
                job.setCompletedAt(now);
                BigDecimal partsCharge = partsRepository.sumPartsChargeByJobId(job.getId());
                job.setPartsCharge(partsCharge != null ? partsCharge : BigDecimal.ZERO);
                // Recalculate actual charge as labor + parts
                BigDecimal labor = job.getLaborCharge() != null ? job.getLaborCharge() : BigDecimal.ZERO;
                job.setActualCharge(labor.add(partsCharge != null ? partsCharge : BigDecimal.ZERO));
                decrementTechActiveJobs(job.getId());
            }
            case CANCELLED   -> {
                job.setCancelledAt(now);
                decrementTechActiveJobs(job.getId());
            }
            default          -> { /* no side effects for other transitions */ }
        }
    }

    private void decrementTechActiveJobs(UUID jobId) {
        // Active job count tracking is best-effort; failures must not roll back the FSM transition.
        // The TechnicianClient call is handled in JobService to avoid circular dependencies.
        // History of assigned technicians is available for audit.
    }

    private void recordHistory(UUID jobId, JobStatus from, JobStatus to, UUID changedBy, String reason) {
        var entry = new JobStatusHistory();
        entry.setJobId(jobId);
        entry.setFromStatus(from.name());
        entry.setToStatus(to.name());
        entry.setChangedBy(changedBy);
        entry.setReason(reason);
        entry.setChangedAt(Instant.now());
        historyRepository.save(entry);
    }
}
