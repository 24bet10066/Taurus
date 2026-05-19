package com.serviceos.job.service;

import com.serviceos.job.feign.AvailableTechnicianDTO;
import com.serviceos.shared.enums.ApplianceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AssignmentEngineTest {

    private AssignmentEngine engine;

    @BeforeEach
    void setUp() {
        engine = new AssignmentEngine();
    }

    private AvailableTechnicianDTO tech(UUID id, List<ApplianceType> skills, int activeJobs, int trust) {
        return new AvailableTechnicianDTO(id, "Tech-" + id, "99999", skills, activeJobs, trust);
    }

    @Test
    @DisplayName("Returns empty when no technicians available")
    void emptyList_returnsEmpty() {
        Optional<UUID> result = engine.findBestTechnician(ApplianceType.AC, Collections.emptyList());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Returns empty when no technician can handle the appliance")
    void noSkillMatch_returnsEmpty() {
        UUID id = UUID.randomUUID();
        var candidates = List.of(tech(id, List.of(ApplianceType.RO), 0, 80));
        // AC needs AC or FRIDGE skill; RO only → no match
        Optional<UUID> result = engine.findBestTechnician(ApplianceType.AC, candidates);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Exact skill match is preferred over related skill")
    void exactSkillBeatsRelatedSkill() {
        UUID exactId = UUID.randomUUID();
        UUID relatedId = UUID.randomUUID();
        var candidates = List.of(
                tech(exactId,   List.of(ApplianceType.AC),     2, 70),
                tech(relatedId, List.of(ApplianceType.FRIDGE), 0, 90)
        );
        // exactId: skill=1.0*0.5 + workload=1/3*0.3 + trust=0.7*0.2 = 0.5+0.1+0.14 = 0.74
        // relatedId: skill=0.5*0.5 + workload=1/1*0.3 + trust=0.9*0.2 = 0.25+0.3+0.18 = 0.73
        Optional<UUID> result = engine.findBestTechnician(ApplianceType.AC, candidates);
        assertThat(result).contains(exactId);
    }

    @Test
    @DisplayName("Lower workload increases score (workloadFactor = 1/(activeJobs+1))")
    void lowerWorkload_higherScore() {
        UUID busyId = UUID.randomUUID();
        UUID freeId = UUID.randomUUID();
        var candidates = List.of(
                tech(busyId, List.of(ApplianceType.AC), 5, 80),
                tech(freeId, List.of(ApplianceType.AC), 0, 80)
        );
        Optional<UUID> result = engine.findBestTechnician(ApplianceType.AC, candidates);
        assertThat(result).contains(freeId);
    }

    @Test
    @DisplayName("Higher trust score breaks workload tie")
    void higherTrust_winsOnTie() {
        UUID lowTrustId = UUID.randomUUID();
        UUID highTrustId = UUID.randomUUID();
        // Same workload, same skill — only trust differs
        var candidates = List.of(
                tech(lowTrustId,  List.of(ApplianceType.WM), 1, 40),
                tech(highTrustId, List.of(ApplianceType.WM), 1, 100)
        );
        Optional<UUID> result = engine.findBestTechnician(ApplianceType.WM, candidates);
        assertThat(result).contains(highTrustId);
    }

    @Test
    @DisplayName("Related skill match returns 0.5 score contribution")
    void relatedSkill_isAccepted() {
        UUID id = UUID.randomUUID();
        // FRIDGE is related to AC
        var candidates = List.of(tech(id, List.of(ApplianceType.FRIDGE), 0, 100));
        Optional<UUID> result = engine.findBestTechnician(ApplianceType.AC, candidates);
        assertThat(result).contains(id);
    }

    @Test
    @DisplayName("Single eligible technician is always selected")
    void singleCandidate_isSelected() {
        UUID id = UUID.randomUUID();
        var candidates = List.of(tech(id, List.of(ApplianceType.GEYSER), 3, 60));
        Optional<UUID> result = engine.findBestTechnician(ApplianceType.GEYSER, candidates);
        assertThat(result).contains(id);
    }

    @Test
    @DisplayName("Score formula: exact match + zero load + perfect trust = maximum score 1.0")
    void perfectCandidate_scoresOne() {
        UUID perfectId = UUID.randomUUID();
        UUID normalId = UUID.randomUUID();
        var candidates = List.of(
                tech(perfectId, List.of(ApplianceType.FRIDGE), 0, 100),
                tech(normalId,  List.of(ApplianceType.FRIDGE), 3, 70)
        );
        Optional<UUID> result = engine.findBestTechnician(ApplianceType.FRIDGE, candidates);
        assertThat(result).contains(perfectId);
    }

    @Test
    @DisplayName("WM and MICROWAVE are related skills")
    void wmMicrowaveRelated() {
        UUID id = UUID.randomUUID();
        var candidates = List.of(tech(id, List.of(ApplianceType.WM), 0, 80));
        Optional<UUID> result = engine.findBestTechnician(ApplianceType.MICROWAVE, candidates);
        assertThat(result).contains(id);
    }

    @Test
    @DisplayName("RO and GEYSER are related skills")
    void roGeyserRelated() {
        UUID id = UUID.randomUUID();
        var candidates = List.of(tech(id, List.of(ApplianceType.RO), 0, 80));
        Optional<UUID> result = engine.findBestTechnician(ApplianceType.GEYSER, candidates);
        assertThat(result).contains(id);
    }
}
