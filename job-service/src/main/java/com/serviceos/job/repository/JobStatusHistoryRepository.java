package com.serviceos.job.repository;

import com.serviceos.job.entity.JobStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobStatusHistoryRepository extends JpaRepository<JobStatusHistory, UUID> {

    List<JobStatusHistory> findByJobIdOrderByChangedAtAsc(UUID jobId);
}
