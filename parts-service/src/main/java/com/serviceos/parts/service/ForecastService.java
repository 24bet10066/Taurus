package com.serviceos.parts.service;

import com.serviceos.parts.dto.response.ForecastResponse;
import com.serviceos.parts.entity.SparePart;
import com.serviceos.parts.repository.InventoryMovementRepository;
import com.serviceos.parts.repository.SparePartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ForecastService {

    private static final Logger log = LoggerFactory.getLogger(ForecastService.class);

    private final SparePartRepository sparePartRepository;
    private final InventoryMovementRepository movementRepository;
    private final PartsEventPublisher eventPublisher;

    @Value("${serviceos.forecast.ewma-alpha:0.30}")
    private double alpha;

    public ForecastService(SparePartRepository sparePartRepository,
                           InventoryMovementRepository movementRepository,
                           PartsEventPublisher eventPublisher) {
        this.sparePartRepository = sparePartRepository;
        this.movementRepository = movementRepository;
        this.eventPublisher = eventPublisher;
    }

    /** Runs every Sunday at 22:00 IST. */
    @Scheduled(cron = "0 0 22 * * SUN", zone = "Asia/Kolkata")
    @Transactional
    public void runWeeklyForecast() {
        log.info("Running weekly EMA forecast (alpha={})", alpha);
        Instant weekEnd = Instant.now();
        Instant weekStart = weekEnd.minus(7, ChronoUnit.DAYS);

        List<SparePart> parts = sparePartRepository.findAllActive();
        int updated = 0;
        int alerts = 0;

        for (SparePart part : parts) {
            int thisWeekUsage = movementRepository.sumOutwardQuantity(
                    part.getId(), weekStart, weekEnd);

            BigDecimal prevForecast = part.getWeeklyForecast() != null
                    ? part.getWeeklyForecast() : BigDecimal.ZERO;

            // EMA: forecast_new = alpha * thisWeek + (1 - alpha) * previousForecast
            double newForecastDouble = alpha * thisWeekUsage + (1 - alpha) * prevForecast.doubleValue();
            BigDecimal newForecast = BigDecimal.valueOf(newForecastDouble)
                    .setScale(2, RoundingMode.HALF_UP);

            // Reorder point = forecast * 3-day lead time + min_stock safety
            int reorderPoint = (int) Math.ceil(newForecastDouble / 7.0 * 3) + part.getMinStock();

            part.setWeeklyForecast(newForecast);
            part.setReorderPoint(reorderPoint);
            sparePartRepository.save(part);
            updated++;

            if (part.getCurrentStock() < reorderPoint) {
                int suggested = Math.max(reorderPoint - part.getCurrentStock(),
                        (int) Math.ceil(newForecastDouble));
                eventPublisher.publishReorderAlert(part, part.getCurrentStock(),
                        thisWeekUsage, suggested);
                alerts++;
            }
        }
        log.info("Forecast complete: {} parts updated, {} reorder alerts fired", updated, alerts);
    }

    @Transactional(readOnly = true)
    public List<ForecastResponse> getForecastSuggestions() {
        return sparePartRepository.findAllActive().stream()
                .filter(p -> p.getCurrentStock() < p.getReorderPoint())
                .map(p -> {
                    int suggested = Math.max(
                            p.getReorderPoint() - p.getCurrentStock(),
                            p.getWeeklyForecast().intValue()
                    );
                    return new ForecastResponse(p.getId(), p.getName(), p.getSku(),
                            p.getCurrentStock(), p.getWeeklyForecast(),
                            p.getReorderPoint(), suggested, true);
                })
                .toList();
    }
}
