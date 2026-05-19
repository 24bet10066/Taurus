package com.serviceos.job.service;

import com.serviceos.job.feign.AvailableTechnicianDTO;
import com.serviceos.shared.enums.ApplianceType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

@Service
public class AssignmentEngine {

    // Related-skill groups: a technician who services any appliance in a group scores 0.5 on related ones.
    private static final List<Set<ApplianceType>> SKILL_GROUPS = List.of(
            Set.of(ApplianceType.AC, ApplianceType.FRIDGE),
            Set.of(ApplianceType.RO, ApplianceType.GEYSER),
            Set.of(ApplianceType.WM, ApplianceType.MICROWAVE)
    );

    /**
     * Scores each available technician and returns the UUID of the best fit.
     * Returns empty if no technician can handle the appliance type.
     *
     * Score = skillMatch * 0.50 + workloadFactor * 0.30 + trustNorm * 0.20
     */
    public Optional<UUID> findBestTechnician(ApplianceType applianceType,
                                             List<AvailableTechnicianDTO> available) {
        if (available == null || available.isEmpty()) return Optional.empty();

        PriorityQueue<AssignmentCandidate> heap = new PriorityQueue<>();
        for (AvailableTechnicianDTO tech : available) {
            double skill = skillMatch(applianceType, tech.skills());
            if (skill == 0.0) continue;

            double workload = 1.0 / (tech.activeJobCount() + 1);
            double trust = tech.trustScore() / 100.0;
            double score = skill * 0.50 + workload * 0.30 + trust * 0.20;
            heap.offer(new AssignmentCandidate(tech.techId(), tech.name(), score));
        }

        if (heap.isEmpty()) return Optional.empty();
        return Optional.of(heap.poll().techId());
    }

    private double skillMatch(ApplianceType required, List<ApplianceType> techSkills) {
        if (techSkills == null) return 0.0;
        if (techSkills.contains(required)) return 1.0;
        for (Set<ApplianceType> group : SKILL_GROUPS) {
            if (group.contains(required) && techSkills.stream().anyMatch(group::contains)) {
                return 0.5;
            }
        }
        return 0.0;
    }

    record AssignmentCandidate(UUID techId, String name, double score)
            implements Comparable<AssignmentCandidate> {
        @Override
        public int compareTo(AssignmentCandidate other) {
            // Reversed so the highest score is at the head of the min-heap.
            return Double.compare(other.score, this.score);
        }
    }
}
