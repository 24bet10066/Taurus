package com.serviceos.job.service;

import com.serviceos.job.entity.Job;
import com.serviceos.job.entity.JobStatusHistory;
import com.serviceos.job.repository.JobPartUsedRepository;
import com.serviceos.job.repository.JobRepository;
import com.serviceos.job.repository.JobStatusHistoryRepository;
import com.serviceos.job.repository.JobTechnicianRepository;
import com.serviceos.shared.enums.JobStatus;
import com.serviceos.shared.result.JobTransitionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobFSMTest {

    @Mock JobRepository jobRepository;
    @Mock JobStatusHistoryRepository historyRepository;
    @Mock JobPartUsedRepository partsRepository;
    @Mock JobTechnicianRepository technicianRepository;
    @Mock JobEventPublisher eventPublisher;

    @InjectMocks JobFSM fsm;

    private UUID jobId;
    private UUID changedBy;
    private Job job;

    @BeforeEach
    void setUp() {
        jobId    = UUID.randomUUID();
        changedBy = UUID.randomUUID();

        job = new Job();
        job.setId(jobId);                                   // set ID so partsRepository lookups work
        job.setStatus(JobStatus.REQUESTED);

        given(jobRepository.findById(jobId)).willReturn(Optional.of(job));
        given(jobRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(partsRepository.sumPartsChargeByJobId(jobId)).willReturn(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Valid transition REQUESTED → ASSIGNED returns Success")
    void validTransition_returnsSuccess() {
        JobTransitionResult result = fsm.transition(jobId, JobStatus.ASSIGNED, changedBy, "assigned");

        assertThat(result).isInstanceOf(JobTransitionResult.Success.class);
        JobTransitionResult.Success success = (JobTransitionResult.Success) result;
        assertThat(success.from()).isEqualTo(JobStatus.REQUESTED);
        assertThat(success.to()).isEqualTo(JobStatus.ASSIGNED);
    }

    @Test
    @DisplayName("Valid transition updates job status in DB")
    void validTransition_savesUpdatedJob() {
        fsm.transition(jobId, JobStatus.ASSIGNED, changedBy, null);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(JobStatus.ASSIGNED);
    }

    @Test
    @DisplayName("Valid transition saves status history record")
    void validTransition_savesHistory() {
        fsm.transition(jobId, JobStatus.ASSIGNED, changedBy, "test reason");

        ArgumentCaptor<JobStatusHistory> captor = ArgumentCaptor.forClass(JobStatusHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getFromStatus()).isEqualTo("REQUESTED");
        assertThat(captor.getValue().getToStatus()).isEqualTo("ASSIGNED");
        assertThat(captor.getValue().getReason()).isEqualTo("test reason");
    }

    @Test
    @DisplayName("Valid transition publishes status-changed event")
    void validTransition_publishesEvent() {
        fsm.transition(jobId, JobStatus.ASSIGNED, changedBy, null);
        verify(eventPublisher).publishStatusChanged(any(), eq(JobStatus.REQUESTED), eq(JobStatus.ASSIGNED));
    }

    @Test
    @DisplayName("Invalid transition returns InvalidTransition, no DB writes")
    void invalidTransition_returnsInvalidTransition() {
        // REQUESTED cannot jump directly to COMPLETED
        JobTransitionResult result = fsm.transition(jobId, JobStatus.COMPLETED, changedBy, null);

        assertThat(result).isInstanceOf(JobTransitionResult.InvalidTransition.class);
        verify(jobRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Transition on non-existent job returns JobNotFound")
    void nonExistentJob_returnsJobNotFound() {
        UUID missing = UUID.randomUUID();
        given(jobRepository.findById(missing)).willReturn(Optional.empty());

        JobTransitionResult result = fsm.transition(missing, JobStatus.ASSIGNED, changedBy, null);

        assertThat(result).isInstanceOf(JobTransitionResult.JobNotFound.class);
    }

    @Test
    @DisplayName("Transition from terminal state CANCELLED returns TerminalState")
    void terminalState_returnsTerminalState() {
        job.setStatus(JobStatus.CANCELLED);

        JobTransitionResult result = fsm.transition(jobId, JobStatus.ASSIGNED, changedBy, null);

        assertThat(result).isInstanceOf(JobTransitionResult.TerminalState.class);
        verify(jobRepository, never()).save(any());
    }

    @Test
    @DisplayName("COMPLETED transition sets completedAt and recalculates partsCharge + actualCharge")
    void completedTransition_setsTimestampAndPartsCharge() {
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setLaborCharge(BigDecimal.valueOf(500));
        given(partsRepository.sumPartsChargeByJobId(jobId)).willReturn(BigDecimal.valueOf(250));

        fsm.transition(jobId, JobStatus.COMPLETED, changedBy, null);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        Job saved = captor.getValue();
        assertThat(saved.getCompletedAt()).isNotNull();
        assertThat(saved.getPartsCharge()).isEqualByComparingTo(BigDecimal.valueOf(250));
        assertThat(saved.getActualCharge()).isEqualByComparingTo(BigDecimal.valueOf(750));
    }

    @Test
    @DisplayName("COMPLETED transition publishes job.completed event")
    void completedTransition_publishesCompletedEvent() {
        job.setStatus(JobStatus.IN_PROGRESS);

        fsm.transition(jobId, JobStatus.COMPLETED, changedBy, null);

        verify(eventPublisher).publishJobCompleted(any());
    }

    @Test
    @DisplayName("CANCELLED transition sets cancelledAt")
    void cancelledTransition_setsCancelledAt() {
        fsm.transition(jobId, JobStatus.CANCELLED, changedBy, "customer cancelled");

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().getCancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("Each step in the full happy path succeeds")
    void fullHappyPath_allTransitionsSucceed() {
        JobStatus[] path = {
                JobStatus.ASSIGNED, JobStatus.IN_TRANSIT, JobStatus.AT_CUSTOMER,
                JobStatus.DIAGNOSING, JobStatus.IN_PROGRESS, JobStatus.COMPLETED
        };
        for (JobStatus next : path) {
            JobTransitionResult result = fsm.transition(jobId, next, changedBy, null);
            assertThat(result)
                    .as("Expected Success for transition to %s", next)
                    .isInstanceOf(JobTransitionResult.Success.class);
        }
    }
}
