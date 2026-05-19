package com.serviceos.job.service;

import com.serviceos.job.dto.response.InvoiceDTO;
import com.serviceos.job.entity.Job;
import com.serviceos.job.entity.JobPartUsed;
import com.serviceos.job.feign.TechnicianClient;
import com.serviceos.job.repository.JobPartUsedRepository;
import com.serviceos.job.repository.JobRepository;
import com.serviceos.job.repository.JobTechnicianRepository;
import com.serviceos.shared.enums.JobStatus;
import com.serviceos.shared.exception.BusinessRuleViolationException;
import com.serviceos.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final JobRepository jobRepository;
    private final JobPartUsedRepository partsRepository;
    private final JobTechnicianRepository technicianRepository;
    private final TechnicianClient technicianClient;

    public InvoiceService(JobRepository jobRepository,
                          JobPartUsedRepository partsRepository,
                          JobTechnicianRepository technicianRepository,
                          TechnicianClient technicianClient) {
        this.jobRepository = jobRepository;
        this.partsRepository = partsRepository;
        this.technicianRepository = technicianRepository;
        this.technicianClient = technicianClient;
    }

    @Transactional(readOnly = true)
    public InvoiceDTO generateInvoice(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new BusinessRuleViolationException("INVOICE_NOT_READY",
                    "Invoice is only available for completed jobs");
        }

        List<JobPartUsed> parts = partsRepository.findByJobId(jobId);
        String technicianName = resolveTechnicianName(jobId);

        List<InvoiceDTO.PartLineItem> partItems = parts.stream()
                .map(p -> new InvoiceDTO.PartLineItem(
                        p.getPartId(),
                        p.getPartName(),
                        p.getQuantity(),
                        p.getUnitCost(),
                        p.getUnitCost().multiply(BigDecimal.valueOf(p.getQuantity()))
                ))
                .toList();

        BigDecimal labor = job.getLaborCharge() != null ? job.getLaborCharge() : BigDecimal.ZERO;
        BigDecimal partsCharge = job.getPartsCharge() != null ? job.getPartsCharge() : BigDecimal.ZERO;

        return new InvoiceDTO(
                job.getId(),
                job.getCustomerName(),
                job.getCustomerPhone(),
                job.getApplianceType(),
                job.getBrand(),
                job.getIssueDescription(),
                technicianName,
                labor,
                partItems,
                labor.add(partsCharge),
                job.getPaymentMethod(),
                job.getCompletedAt(),
                job.getCompletedAt() != null
                        ? job.getCompletedAt().plus(30, ChronoUnit.DAYS)
                        : null
        );
    }

    private String resolveTechnicianName(UUID jobId) {
        return technicianRepository.findByJobIdAndRole(jobId, "PRIMARY")
                .map(jt -> {
                    try {
                        return technicianClient.getTechnicianDetail(jt.getId().getTechnicianId()).name();
                    } catch (Exception ex) {
                        log.warn("Could not fetch technician name for invoice jobId={}: {}", jobId, ex.getMessage());
                        return null;
                    }
                })
                .orElse(null);
    }
}
