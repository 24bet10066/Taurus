package com.serviceos.parts.service;

import com.serviceos.parts.entity.SparePart;
import com.serviceos.shared.enums.MovementType;
import com.serviceos.shared.event.CreditUpdatedEvent;
import com.serviceos.shared.event.InventoryReorderEvent;
import com.serviceos.shared.event.PartsSoldEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class PartsEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PartsEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PartsEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPartsSold(UUID saleId, UUID partId, String sku, UUID buyerId,
                                 int qty, BigDecimal unitPrice, BigDecimal total,
                                 MovementType movementType) {
        var event = new PartsSoldEvent(saleId, partId, sku, buyerId, "FREELANCE",
                null, qty, unitPrice, total, movementType, Instant.now());
        send(PartsSoldEvent.TOPIC, saleId.toString(), event);
    }

    public void publishReorderAlert(SparePart part, int currentStock, int forecastedDemand, int suggestedQty) {
        var event = new InventoryReorderEvent(
                part.getId(), part.getSku(), part.getName(),
                currentStock, part.getReorderPoint(),
                forecastedDemand, suggestedQty, Instant.now()
        );
        send(InventoryReorderEvent.TOPIC, part.getId().toString(), event);
    }

    public void publishCreditUpdated(UUID techId, BigDecimal delta, BigDecimal newBalance,
                                     BigDecimal creditLimit, String reason, UUID referenceId) {
        var event = new CreditUpdatedEvent(techId, delta, newBalance, creditLimit, reason,
                referenceId, Instant.now());
        send(CreditUpdatedEvent.TOPIC, techId.toString(), event);
    }

    private void send(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Failed to publish to {}: {}", topic, ex.getMessage());
                });
    }
}
