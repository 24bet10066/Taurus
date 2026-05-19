package com.serviceos.shared.enums;

import java.util.Set;

public enum JobStatus {
    REQUESTED,
    ASSIGNED,
    IN_TRANSIT,
    AT_CUSTOMER,
    DIAGNOSING,
    PARTS_NEEDED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    REVISIT_NEEDED;

    public boolean canTransitionTo(JobStatus next) {
        return switch (this) {
            case REQUESTED     -> Set.of(ASSIGNED, CANCELLED).contains(next);
            case ASSIGNED      -> Set.of(IN_TRANSIT, CANCELLED).contains(next);
            case IN_TRANSIT    -> Set.of(AT_CUSTOMER, CANCELLED).contains(next);
            case AT_CUSTOMER   -> Set.of(DIAGNOSING, CANCELLED).contains(next);
            case DIAGNOSING    -> Set.of(PARTS_NEEDED, IN_PROGRESS, CANCELLED).contains(next);
            case PARTS_NEEDED  -> Set.of(IN_PROGRESS, CANCELLED).contains(next);
            case IN_PROGRESS   -> Set.of(COMPLETED, CANCELLED).contains(next);
            case COMPLETED     -> next == REVISIT_NEEDED;
            case REVISIT_NEEDED-> Set.of(ASSIGNED, CANCELLED).contains(next);
            case CANCELLED     -> false;
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
