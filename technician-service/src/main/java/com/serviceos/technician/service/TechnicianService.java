package com.serviceos.technician.service;

import com.serviceos.shared.dto.PageResponse;
import com.serviceos.shared.enums.ApplianceType;
import com.serviceos.shared.enums.TechnicianType;
import com.serviceos.shared.exception.ResourceNotFoundException;
import com.serviceos.technician.dto.request.CreateTechnicianRequest;
import com.serviceos.technician.dto.request.UpdateTechnicianRequest;
import com.serviceos.technician.dto.response.TechnicianResponse;
import com.serviceos.technician.entity.Technician;
import com.serviceos.technician.repository.TechnicianRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TechnicianService {

    private final TechnicianRepository technicianRepository;

    public TechnicianService(TechnicianRepository technicianRepository) {
        this.technicianRepository = technicianRepository;
    }

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    @Transactional
    public TechnicianResponse create(CreateTechnicianRequest req) {
        Technician t = new Technician();
        t.setName(req.name());
        t.setPhone(req.phone());
        t.setEmail(req.email());
        t.setType(req.type());
        t.setSkills(req.skills().stream().map(ApplianceType::name).toList());
        t.setCity(req.city());
        t.setPincode(req.pincode());
        return toResponse(technicianRepository.save(t));
    }

    @Transactional
    public TechnicianResponse update(UUID id, UpdateTechnicianRequest req) {
        Technician t = requireTechnician(id);
        if (req.name()     != null) t.setName(req.name());
        if (req.email()    != null) t.setEmail(req.email());
        if (req.skills()   != null) t.setSkills(req.skills().stream().map(ApplianceType::name).toList());
        if (req.city()     != null) t.setCity(req.city());
        if (req.pincode()  != null) t.setPincode(req.pincode());
        if (req.status()   != null) t.setStatus(req.status());
        if (req.approved() != null) t.setApproved(req.approved());
        if (req.active()   != null) t.setActive(req.active());
        return toResponse(technicianRepository.save(t));
    }

    @Transactional(readOnly = true)
    public TechnicianResponse getById(UUID id) {
        return toResponse(requireTechnician(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<TechnicianResponse> list(String query, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("name"));
        Page<Technician> p = (query != null && !query.isBlank())
                ? technicianRepository.search(query, pr)
                : technicianRepository.findByActiveTrue(pr);
        return PageResponse.of(p.getContent().stream().map(this::toResponse).toList(),
                page, size, p.getTotalElements());
    }

    @Transactional(readOnly = true)
    public TechnicianResponse getByPhone(String phone) {
        return toResponse(technicianRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Technician", phone)));
    }

    // -------------------------------------------------------------------------
    // Internal: active-job counter (called by job-service via REST)
    // -------------------------------------------------------------------------

    @Transactional
    public void adjustActiveJobs(UUID id, int delta) {
        Technician t = requireTechnician(id);
        t.setActiveJobs(Math.max(0, t.getActiveJobs() + delta));
        technicianRepository.save(t);
    }

    // -------------------------------------------------------------------------
    // Internal: available technicians by skill (called by job-service)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Technician> findAvailableBySkill(String applianceType) {
        return technicianRepository.findActiveBySkill(applianceType);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    public Technician requireTechnician(UUID id) {
        return technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician", id));
    }

    public int countActiveApproved() {
        return (int) technicianRepository.findAll().stream()
                .filter(t -> t.isActive() && t.isApproved())
                .count();
    }

    TechnicianResponse toResponse(Technician t) {
        List<ApplianceType> skills = t.getSkills().stream()
                .map(ApplianceType::valueOf).toList();
        return new TechnicianResponse(
                t.getId(), t.getName(), t.getPhone(), t.getEmail(),
                t.getType(), t.getStatus(), skills,
                t.getCity(), t.getPincode(),
                t.getActiveJobs(), t.getTotalJobsCompleted(),
                t.getTotalPartsPurchased(), t.getTotalPartsPaid(),
                t.getTrustScore().multiply(java.math.BigDecimal.valueOf(100)).intValue(),
                t.getCreditLimit(), t.isApproved(), t.isActive(),
                t.getOnboardedAt(), t.getCreatedAt()
        );
    }
}
