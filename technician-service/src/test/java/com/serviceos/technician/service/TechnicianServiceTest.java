package com.serviceos.technician.service;

import com.serviceos.shared.enums.ApplianceType;
import com.serviceos.shared.enums.TechnicianType;
import com.serviceos.shared.exception.ResourceNotFoundException;
import com.serviceos.technician.dto.request.CreateTechnicianRequest;
import com.serviceos.technician.dto.response.TechnicianResponse;
import com.serviceos.technician.entity.Technician;
import com.serviceos.technician.repository.TechnicianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class TechnicianServiceTest {

    @Mock TechnicianRepository technicianRepository;

    @InjectMocks TechnicianService technicianService;

    private Technician technician;
    private final UUID techId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        technician = new Technician();
        technician.setId(techId);
        technician.setName("Raju Kumar");
        technician.setPhone("9876543210");
        technician.setType(TechnicianType.FREELANCE);
        technician.setStatus("ACTIVE");
        technician.setSkills(List.of("AC", "FRIDGE"));
        technician.setActiveJobs(2);
        technician.setTotalJobsCompleted(15);
        technician.setTotalPartsPurchased(new BigDecimal("5000"));
        technician.setTotalPartsPaid(new BigDecimal("4000"));
        technician.setPartsOrderCount(8);
        technician.setTrustScore(BigDecimal.valueOf(0.72));
        technician.setCreditLimit(BigDecimal.valueOf(3000));
        technician.setApproved(true);
        technician.setActive(true);
        technician.setOnboardedAt(Instant.now());
        technician.setCreatedAt(Instant.now());
    }

    @Test
    void create_savesAndReturnsResponse() {
        when(technicianRepository.save(any())).thenAnswer(inv -> {
            Technician t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            t.setCreatedAt(Instant.now());
            t.setOnboardedAt(Instant.now());
            return t;
        });

        TechnicianResponse response = technicianService.create(
                new CreateTechnicianRequest("Test Tech", "9000000000", null,
                        TechnicianType.FREELANCE,
                        List.of(ApplianceType.AC, ApplianceType.WM),
                        "Delhi", "110001"));

        assertThat(response.name()).isEqualTo("Test Tech");
        assertThat(response.type()).isEqualTo(TechnicianType.FREELANCE);
        assertThat(response.skills()).containsExactlyInAnyOrder(ApplianceType.AC, ApplianceType.WM);
        verify(technicianRepository).save(any());
    }

    @Test
    void getById_notFound_throwsException() {
        UUID missing = UUID.randomUUID();
        when(technicianRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> technicianService.getById(missing))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_found_returnsResponse() {
        when(technicianRepository.findById(techId)).thenReturn(Optional.of(technician));

        TechnicianResponse response = technicianService.getById(techId);

        assertThat(response.id()).isEqualTo(techId);
        assertThat(response.name()).isEqualTo("Raju Kumar");
        assertThat(response.trustScorePercent()).isEqualTo(72);
    }

    @Test
    void trustScorePercent_isConvertedFrom0to100Scale() {
        when(technicianRepository.findById(techId)).thenReturn(Optional.of(technician));
        technician.setTrustScore(BigDecimal.valueOf(0.85));

        TechnicianResponse response = technicianService.getById(techId);

        assertThat(response.trustScorePercent()).isEqualTo(85);
    }

    @Test
    void adjustActiveJobs_increment() {
        when(technicianRepository.findById(techId)).thenReturn(Optional.of(technician));
        when(technicianRepository.save(any())).thenReturn(technician);

        technicianService.adjustActiveJobs(techId, 1);

        assertThat(technician.getActiveJobs()).isEqualTo(3);
        verify(technicianRepository).save(technician);
    }

    @Test
    void adjustActiveJobs_decrement() {
        when(technicianRepository.findById(techId)).thenReturn(Optional.of(technician));
        when(technicianRepository.save(any())).thenReturn(technician);

        technicianService.adjustActiveJobs(techId, -1);

        assertThat(technician.getActiveJobs()).isEqualTo(1);
    }

    @Test
    void adjustActiveJobs_doesNotGoBelowZero() {
        technician.setActiveJobs(0);
        when(technicianRepository.findById(techId)).thenReturn(Optional.of(technician));
        when(technicianRepository.save(any())).thenReturn(technician);

        technicianService.adjustActiveJobs(techId, -5);

        assertThat(technician.getActiveJobs()).isEqualTo(0);
    }

    @Test
    void skillsRoundTrip_preservesApplianceTypes() {
        when(technicianRepository.findById(techId)).thenReturn(Optional.of(technician));

        TechnicianResponse response = technicianService.getById(techId);

        assertThat(response.skills()).containsExactlyInAnyOrder(ApplianceType.AC, ApplianceType.FRIDGE);
    }
}
