package com.serviceos.analytics.consumer;

import com.serviceos.analytics.entity.DailyJobMetrics;
import com.serviceos.analytics.entity.DailyJobMetricsId;
import com.serviceos.analytics.repository.DailyJobMetricsRepository;
import com.serviceos.shared.event.PaymentReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class PaymentReceivedConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentReceivedConsumer.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    // Aggregate revenue row — no appliance/area breakdown from payment events
    private static final String AGGREGATE_KEY = "ALL";

    private final DailyJobMetricsRepository metricsRepository;

    public PaymentReceivedConsumer(DailyJobMetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    @KafkaListener(topics = PaymentReceivedEvent.TOPIC, groupId = "analytics-service")
    @Transactional
    public void onPaymentReceived(PaymentReceivedEvent event) {
        try {
            LocalDate today = event.receivedAt().atZone(IST).toLocalDate();
            DailyJobMetrics row = metricsRepository
                    .findById(new DailyJobMetricsId(today, AGGREGATE_KEY, AGGREGATE_KEY))
                    .orElseGet(() -> new DailyJobMetrics(today, AGGREGATE_KEY, AGGREGATE_KEY));

            row.setTotalRevenue(row.getTotalRevenue().add(event.amount()));
            metricsRepository.save(row);
        } catch (Exception ex) {
            log.error("Error processing payment.received event {}: {}", event.paymentId(), ex.getMessage(), ex);
        }
    }
}
