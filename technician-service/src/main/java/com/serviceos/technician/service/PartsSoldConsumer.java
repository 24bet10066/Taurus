package com.serviceos.technician.service;

import com.serviceos.shared.enums.MovementType;
import com.serviceos.shared.event.PartsSoldEvent;
import com.serviceos.technician.entity.Technician;
import com.serviceos.technician.repository.TechnicianRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PartsSoldConsumer {

    private static final Logger log = LoggerFactory.getLogger(PartsSoldConsumer.class);

    private final TechnicianRepository technicianRepository;

    public PartsSoldConsumer(TechnicianRepository technicianRepository) {
        this.technicianRepository = technicianRepository;
    }

    @KafkaListener(topics = PartsSoldEvent.TOPIC, groupId = "technician-service")
    @Transactional
    public void onPartsSold(PartsSoldEvent event) {
        // Only track B2B sales to freelancers
        if (event.movementType() != MovementType.OUTWARD_B2B) return;
        if (event.buyerId() == null) return;

        log.debug("parts.sold received: saleId={} buyerId={} amount={}",
                event.saleId(), event.buyerId(), event.totalAmount());

        technicianRepository.findById(event.buyerId()).ifPresent(t -> {
            t.setTotalPartsPurchased(t.getTotalPartsPurchased().add(event.totalAmount()));
            t.setPartsOrderCount(t.getPartsOrderCount() + 1);
            technicianRepository.save(t);
        });
    }
}
