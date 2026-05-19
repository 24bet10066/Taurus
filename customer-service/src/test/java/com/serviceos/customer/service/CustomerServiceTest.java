package com.serviceos.customer.service;

import com.serviceos.customer.dto.request.CreateApplianceRequest;
import com.serviceos.customer.dto.request.UpsertCustomerRequest;
import com.serviceos.customer.dto.response.CustomerResponse;
import com.serviceos.customer.entity.Customer;
import com.serviceos.customer.entity.CustomerAppliance;
import com.serviceos.customer.repository.CustomerApplianceRepository;
import com.serviceos.customer.repository.CustomerRepository;
import com.serviceos.shared.enums.ApplianceType;
import com.serviceos.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock CustomerRepository customerRepository;
    @Mock CustomerApplianceRepository applianceRepository;

    @InjectMocks CustomerService customerService;

    private Customer existingCustomer;
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        existingCustomer = new Customer();
        existingCustomer.setId(customerId);  // simulate post-persist
        existingCustomer.setName("Test User");
        existingCustomer.setPhone("9999999999");
        existingCustomer.setTotalSpent(BigDecimal.ZERO);
        existingCustomer.setJobCount(0);
        existingCustomer.setActive(true);
    }

    @Test
    void upsert_newCustomer_createsRecord() {
        when(customerRepository.findByPhone("9876543210")).thenReturn(Optional.empty());
        when(customerRepository.save(any())).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CustomerResponse response = customerService.upsert(
                new UpsertCustomerRequest("9876543210", "New User", null, null, null, null));

        assertThat(response.phone()).isEqualTo("9876543210");
        assertThat(response.name()).isEqualTo("New User");
        verify(customerRepository).save(any());
    }

    @Test
    void upsert_existingCustomer_updatesName() {
        when(customerRepository.findByPhone("9999999999")).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any())).thenReturn(existingCustomer);

        CustomerResponse response = customerService.upsert(
                new UpsertCustomerRequest("9999999999", "Updated Name", null, null, null, null));

        assertThat(response.name()).isEqualTo("Updated Name");
    }

    @Test
    void getByPhone_notFound_throwsException() {
        when(customerRepository.findByPhone("0000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getByPhone("0000000000"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_notFound_throwsException() {
        UUID missing = UUID.randomUUID();
        when(customerRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getById(missing))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void recordJobCompletion_updatesStatsAndNextServiceDue() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any())).thenReturn(existingCustomer);

        BigDecimal amount = new BigDecimal("1500.00");
        Instant now = Instant.now();
        customerService.recordJobCompletion(customerId, amount, now);

        assertThat(existingCustomer.getTotalSpent()).isEqualByComparingTo(amount);
        assertThat(existingCustomer.getJobCount()).isEqualTo(1);
        assertThat(existingCustomer.getLastServiceDate()).isEqualTo(now);
        assertThat(existingCustomer.getNextServiceDue()).isNotNull();
        verify(customerRepository).save(existingCustomer);
    }

    @Test
    void recordJobCompletion_customerNotFound_doesNothing() {
        UUID missingId = UUID.randomUUID();
        when(customerRepository.findById(missingId)).thenReturn(Optional.empty());

        customerService.recordJobCompletion(missingId, BigDecimal.TEN, Instant.now());

        verify(customerRepository, never()).save(any());
    }

    @Test
    void recordJobCompletion_accumulatesTotalSpent() {
        existingCustomer.setTotalSpent(new BigDecimal("500.00"));
        existingCustomer.setJobCount(2);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any())).thenReturn(existingCustomer);

        customerService.recordJobCompletion(customerId, new BigDecimal("300.00"), Instant.now());

        assertThat(existingCustomer.getTotalSpent()).isEqualByComparingTo("800.00");
        assertThat(existingCustomer.getJobCount()).isEqualTo(3);
    }

    @Test
    void addAppliance_customerNotFound_throwsException() {
        UUID missing = UUID.randomUUID();
        when(customerRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.addAppliance(missing,
                new CreateApplianceRequest(ApplianceType.AC, "Samsung", null, null,
                        null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addAppliance_savesWithCorrectCustomerId() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(applianceRepository.save(any())).thenAnswer(inv -> {
            CustomerAppliance a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        var response = customerService.addAppliance(customerId,
                new CreateApplianceRequest(ApplianceType.FRIDGE, "LG", "GL-T302", null,
                        null, null, null, null, "Main kitchen fridge"));

        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.applianceType()).isEqualTo(ApplianceType.FRIDGE);
        assertThat(response.brand()).isEqualTo("LG");
    }
}
