package com.serviceos.analytics.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class DailyJobMetricsId implements Serializable {

    private LocalDate metricDate;
    private String applianceType;
    private String area;

    public DailyJobMetricsId() {}

    public DailyJobMetricsId(LocalDate metricDate, String applianceType, String area) {
        this.metricDate = metricDate;
        this.applianceType = applianceType;
        this.area = area;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyJobMetricsId that)) return false;
        return Objects.equals(metricDate, that.metricDate)
                && Objects.equals(applianceType, that.applianceType)
                && Objects.equals(area, that.area);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metricDate, applianceType, area);
    }
}
