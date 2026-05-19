package com.serviceos.job.service;

import com.serviceos.job.entity.Job;
import com.serviceos.job.feign.TechnicianClient;
import com.serviceos.job.feign.TechnicianDetailDTO;
import com.serviceos.job.repository.JobTechnicianRepository;
import com.serviceos.shared.enums.JobStatus;
import com.serviceos.shared.enums.TechnicianType;
import com.serviceos.shared.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class JobEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(JobEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JobTechnicianRepository jobTechnicianRepository;
    private final TechnicianClient technicianClient;

    public JobEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                             JobTechnicianRepository jobTechnicianRepository,
                             TechnicianClient technicianClient) {
        this.kafkaTemplate = kafkaTemplate;
        this.jobTechnicianRepository = jobTechnicianRepository;
        this.technicianClient = technicianClient;
    }

    public void publishJobCreated(Job job) {
        var event = new JobCreatedEvent(
                job.getId(), job.getCustomerId(),
                job.getCustomerName(), job.getCustomerPhone(),
                job.getApplianceType(), job.getIssueDescription(),
                job.getArea(), null,
                job.getCreatedAt(), null
        );
        send(JobCreatedEvent.TOPIC, job.getId().toString(), event);
    }

    public void publishStatusChanged(Job job, JobStatus from, JobStatus to) {
        UUID primaryTechId = jobTechnicianRepository
                .findByJobIdAndRole(job.getId(), "PRIMARY")
                .map(jt -> jt.getId().getTechnicianId())
                .orElse(null);

        String techName = null;
        String techPhone = null;
        if (primaryTechId != null) {
            try {
                TechnicianDetailDTO detail = technicianClient.getTechnicianDetail(primaryTechId);
                techName = detail.name();
                techPhone = detail.phone();
            } catch (Exception ex) {
                log.warn("Could not fetch technician detail for notification enrichment: {}", ex.getMessage());
            }
        }

        BigDecimal totalCharge = null;
        if (to == JobStatus.COMPLETED) {
            BigDecimal labor = job.getLaborCharge() != null ? job.getLaborCharge() : BigDecimal.ZERO;
            BigDecimal parts = job.getPartsCharge() != null ? job.getPartsCharge() : BigDecimal.ZERO;
            totalCharge = labor.add(parts);
        }

        var event = new JobStatusChangedEvent(
                job.getId(), job.getCustomerId(), primaryTechId,
                from, to, null, Instant.now(),
                job.getCustomerPhone(), techName, techPhone,
                job.getApplianceType(), totalCharge, null
        );
        send(JobStatusChangedEvent.TOPIC, job.getId().toString(), event);

        if (to == JobStatus.ASSIGNED && primaryTechId != null) {
            publishJobAssigned(job, primaryTechId, techName, techPhone);
        }
    }

    public void publishJobCompleted(Job job) {
        UUID primaryTechId = jobTechnicianRepository
                .findByJobIdAndRole(job.getId(), "PRIMARY")
                .map(jt -> jt.getId().getTechnicianId())
                .orElse(null);

        BigDecimal labor = job.getLaborCharge() != null ? job.getLaborCharge() : BigDecimal.ZERO;
        BigDecimal parts = job.getPartsCharge() != null ? job.getPartsCharge() : BigDecimal.ZERO;
        BigDecimal total = labor.add(parts);

        var event = new JobCompletedEvent(
                job.getId(), job.getCustomerId(), primaryTechId,
                labor, parts, total, 30,
                job.getCompletedAt(),
                job.getApplianceType(),
                job.getArea()
        );
        send(JobCompletedEvent.TOPIC, job.getId().toString(), event);
    }

    private void publishJobAssigned(Job job, UUID techId, String techName, String techPhone) {
        var event = new JobAssignedEvent(
                job.getId(), techId, techPhone,
                TechnicianType.HIRED, // assumed hired; type is not on Job entity
                job.getCustomerName(), job.getCustomerPhone(),
                job.getArea(), job.getApplianceType(),
                job.getIssueDescription(), Instant.now(), null
        );
        send(JobAssignedEvent.TOPIC, job.getId().toString(), event);
    }

    private void send(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) log.error("Failed to publish to {}: {}", topic, ex.getMessage());
                    else log.debug("Published to {} offset={}", topic, result.getRecordMetadata().offset());
                });
    }
}
