package com.serviceos.customer.service;

import com.serviceos.customer.entity.Customer;
import com.serviceos.customer.entity.CustomerAppliance;
import com.serviceos.customer.event.AmcOpportunityEvent;
import com.serviceos.customer.repository.CustomerApplianceRepository;
import com.serviceos.customer.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Component
public class AmcScheduler {

    private static final Logger log = LoggerFactory.getLogger(AmcScheduler.class);

    private final CustomerApplianceRepository applianceRepository;
    private final CustomerRepository customerRepository;
    private final KafkaTemplate<String, Object> kafka;

    public AmcScheduler(CustomerApplianceRepository applianceRepository,
                        CustomerRepository customerRepository,
                        KafkaTemplate<String, Object> kafka) {
        this.applianceRepository = applianceRepository;
        this.customerRepository  = customerRepository;
        this.kafka               = kafka;
    }

    @Scheduled(cron = "0 0 9 * * MON", zone = "Asia/Kolkata")
    public void publishAmcOpportunities() {
        LocalDate today = LocalDate.now();
        LocalDate in14  = today.plusDays(14);
        LocalDate in30  = today.plusDays(30);

        List<CustomerAppliance> serviceDue = applianceRepository.findServiceDueInRange(today, in14);
        log.info("AMC scheduler: {} appliances due for service in 14 days", serviceDue.size());
        serviceDue.forEach(a -> publish(a, AmcOpportunityEvent.REASON_SERVICE_DUE, a.getNextServiceDue()));

        List<CustomerAppliance> amcExpiring = applianceRepository.findAmcExpiringInRange(today, in30);
        log.info("AMC scheduler: {} appliances with AMC expiring in 30 days", amcExpiring.size());
        amcExpiring.forEach(a -> publish(a, AmcOpportunityEvent.REASON_AMC_EXPIRING, a.getAmcEndDate()));
    }

    private void publish(CustomerAppliance appliance, String reason, LocalDate dueDate) {
        customerRepository.findById(appliance.getCustomerId()).ifPresent(customer -> {
            var event = new AmcOpportunityEvent(
                    customer.getId(), customer.getName(), customer.getPhone(),
                    appliance.getId(), appliance.getApplianceType(), appliance.getBrand(),
                    reason, dueDate, Instant.now()
            );
            kafka.send(AmcOpportunityEvent.TOPIC, customer.getId().toString(), event);
            log.debug("Published AMC opportunity: customerId={} reason={}", customer.getId(), reason);
        });
    }
}
