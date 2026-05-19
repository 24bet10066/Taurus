package com.serviceos.technician.service;

import com.serviceos.shared.event.CreditUpdatedEvent;
import com.serviceos.technician.repository.TechnicianRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreditUpdatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(CreditUpdatedConsumer.class);

    private final TechnicianRepository technicianRepository;
    private final TrustScoreService trustScoreService;

    public CreditUpdatedConsumer(TechnicianRepository technicianRepository,
                                 TrustScoreService trustScoreService) {
        this.technicianRepository = technicianRepository;
        this.trustScoreService    = trustScoreService;
    }

    @KafkaListener(topics = CreditUpdatedEvent.TOPIC, groupId = "technician-service")
    @Transactional
    public void onCreditUpdated(CreditUpdatedEvent event) {
        log.debug("credit.updated received: technicianId={} delta={} reason={}",
                event.technicianId(), event.delta(), event.reason());

        // Track payments (negative delta = payment received)
        if (event.delta().compareTo(java.math.BigDecimal.ZERO) < 0) {
            technicianRepository.findById(event.technicianId()).ifPresent(t -> {
                t.setTotalPartsPaid(t.getTotalPartsPaid().add(event.delta().abs()));
                technicianRepository.save(t);
                // Trigger trust score recompute after payment
                trustScoreService.recompute(t, true);
            });
        }
    }
}
