package com.serviceos.analytics.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class TechnicianPerformanceId implements Serializable {

    private UUID technicianId;
    private LocalDate metricMonth;

    public TechnicianPerformanceId() {}

    public TechnicianPerformanceId(UUID technicianId, LocalDate metricMonth) {
        this.technicianId = technicianId;
        this.metricMonth = metricMonth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TechnicianPerformanceId that)) return false;
        return Objects.equals(technicianId, that.technicianId)
                && Objects.equals(metricMonth, that.metricMonth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(technicianId, metricMonth);
    }
}
