package com.serviceos.analytics.consumer;

import com.serviceos.analytics.entity.InventorySnapshot;
import com.serviceos.analytics.repository.InventorySnapshotRepository;
import com.serviceos.shared.event.PartsSoldEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class PartsSoldConsumer {

    private static final Logger log = LoggerFactory.getLogger(PartsSoldConsumer.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final InventorySnapshotRepository snapshotRepository;

    public PartsSoldConsumer(InventorySnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @KafkaListener(topics = PartsSoldEvent.TOPIC, groupId = "analytics-service")
    @Transactional
    public void onPartsSold(PartsSoldEvent event) {
        try {
            LocalDate today = event.soldAt().atZone(IST).toLocalDate();
            InventorySnapshot snapshot = snapshotRepository
                    .findBySnapshotDateAndPartId(today, event.partId())
                    .orElseGet(() -> {
                        InventorySnapshot s = new InventorySnapshot(today, event.partId());
                        s.setPartName(event.sku());
                        return s;
                    });
            snapshot.setPartsSoldThisWeek(snapshot.getPartsSoldThisWeek() + event.quantity());
            snapshotRepository.save(snapshot);
        } catch (Exception ex) {
            log.error("Error processing parts.sold event {}: {}", event.saleId(), ex.getMessage(), ex);
        }
    }
}
