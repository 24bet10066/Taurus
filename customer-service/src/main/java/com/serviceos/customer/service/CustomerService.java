package com.serviceos.customer.service;

import com.serviceos.customer.dto.request.CreateApplianceRequest;
import com.serviceos.customer.dto.request.UpdateApplianceRequest;
import com.serviceos.customer.dto.request.UpdateCustomerRequest;
import com.serviceos.customer.dto.request.UpsertCustomerRequest;
import com.serviceos.customer.dto.response.ApplianceResponse;
import com.serviceos.customer.dto.response.CustomerProfileResponse;
import com.serviceos.customer.dto.response.CustomerResponse;
import com.serviceos.customer.entity.Customer;
import com.serviceos.customer.entity.CustomerAppliance;
import com.serviceos.customer.repository.CustomerApplianceRepository;
import com.serviceos.customer.repository.CustomerRepository;
import com.serviceos.shared.dto.PageResponse;
import com.serviceos.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerApplianceRepository applianceRepository;

    public CustomerService(CustomerRepository customerRepository,
                           CustomerApplianceRepository applianceRepository) {
        this.customerRepository = customerRepository;
        this.applianceRepository = applianceRepository;
    }

    // -------------------------------------------------------------------------
    // Upsert
    // -------------------------------------------------------------------------

    @Transactional
    public CustomerResponse upsert(UpsertCustomerRequest req) {
        Customer c = customerRepository.findByPhone(req.phone())
                .orElseGet(Customer::new);
        c.setPhone(req.phone());
        c.setName(req.name());
        if (req.email()   != null) c.setEmail(req.email());
        if (req.address() != null) c.setAddress(req.address());
        if (req.city()    != null) c.setCity(req.city());
        if (req.pincode() != null) c.setPincode(req.pincode());
        return toResponse(customerRepository.save(c));
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> list(String query, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("name"));
        Page<Customer> p = (query != null && !query.isBlank())
                ? customerRepository.search(query, pr)
                : customerRepository.findByActiveTrue(pr);
        return PageResponse.of(p.getContent().stream().map(this::toResponse).toList(),
                page, size, p.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(UUID id) {
        return toResponse(requireCustomer(id));
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile(UUID id) {
        Customer c = requireCustomer(id);
        List<ApplianceResponse> appliances = applianceRepository.findByCustomerId(id)
                .stream().map(this::toApplianceResponse).toList();
        return new CustomerProfileResponse(toResponse(c), appliances);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getByPhone(String phone) {
        return toResponse(customerRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", phone)));
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Transactional
    public CustomerResponse update(UUID id, UpdateCustomerRequest req) {
        Customer c = requireCustomer(id);
        if (req.name()    != null) c.setName(req.name());
        if (req.email()   != null) c.setEmail(req.email());
        if (req.address() != null) c.setAddress(req.address());
        if (req.city()    != null) c.setCity(req.city());
        if (req.pincode() != null) c.setPincode(req.pincode());
        return toResponse(customerRepository.save(c));
    }

    // -------------------------------------------------------------------------
    // Appliances
    // -------------------------------------------------------------------------

    @Transactional
    public ApplianceResponse addAppliance(UUID customerId, CreateApplianceRequest req) {
        requireCustomer(customerId);
        CustomerAppliance a = new CustomerAppliance();
        a.setCustomerId(customerId);
        a.setApplianceType(req.applianceType());
        a.setBrand(req.brand());
        a.setModel(req.model());
        a.setSerialNumber(req.serialNumber());
        a.setPurchaseDate(req.purchaseDate());
        a.setAmcStartDate(req.amcStartDate());
        a.setAmcEndDate(req.amcEndDate());
        a.setNextServiceDue(req.nextServiceDue());
        a.setNotes(req.notes());
        return toApplianceResponse(applianceRepository.save(a));
    }

    @Transactional(readOnly = true)
    public List<ApplianceResponse> listAppliances(UUID customerId) {
        requireCustomer(customerId);
        return applianceRepository.findByCustomerId(customerId)
                .stream().map(this::toApplianceResponse).toList();
    }

    @Transactional
    public ApplianceResponse updateAppliance(UUID customerId, UUID applianceId,
                                              UpdateApplianceRequest req) {
        CustomerAppliance a = applianceRepository.findByIdAndCustomerId(applianceId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerAppliance", applianceId));
        if (req.brand()          != null) a.setBrand(req.brand());
        if (req.model()          != null) a.setModel(req.model());
        if (req.serialNumber()   != null) a.setSerialNumber(req.serialNumber());
        if (req.purchaseDate()   != null) a.setPurchaseDate(req.purchaseDate());
        if (req.amcStartDate()   != null) a.setAmcStartDate(req.amcStartDate());
        if (req.amcEndDate()     != null) a.setAmcEndDate(req.amcEndDate());
        if (req.nextServiceDue() != null) a.setNextServiceDue(req.nextServiceDue());
        if (req.notes()          != null) a.setNotes(req.notes());
        return toApplianceResponse(applianceRepository.save(a));
    }

    @Transactional
    public void deleteAppliance(UUID customerId, UUID applianceId) {
        CustomerAppliance a = applianceRepository.findByIdAndCustomerId(applianceId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerAppliance", applianceId));
        applianceRepository.delete(a);
    }

    // -------------------------------------------------------------------------
    // Internal: called by Kafka consumer
    // -------------------------------------------------------------------------

    @Transactional
    public void recordJobCompletion(UUID customerId, BigDecimal totalAmount, Instant completedAt) {
        customerRepository.findById(customerId).ifPresent(c -> {
            c.setTotalSpent(c.getTotalSpent().add(totalAmount));
            c.setJobCount(c.getJobCount() + 1);
            c.setLastServiceDate(completedAt);
            // Default 6-month follow-up; appliance-level scheduling uses CustomerAppliance.nextServiceDue
            c.setNextServiceDue(completedAt.atZone(java.time.ZoneId.of("Asia/Kolkata"))
                    .toLocalDate().plusMonths(6));
            customerRepository.save(c);
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    public Customer requireCustomer(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(
                c.getId(), c.getName(), c.getPhone(), c.getEmail(),
                c.getAddress(), c.getCity(), c.getPincode(),
                c.getTotalSpent(), c.getJobCount(), c.getLastServiceDate(),
                c.getNextServiceDue(), c.isActive(), c.getCreatedAt()
        );
    }

    ApplianceResponse toApplianceResponse(CustomerAppliance a) {
        return new ApplianceResponse(
                a.getId(), a.getCustomerId(), a.getApplianceType(),
                a.getBrand(), a.getModel(), a.getSerialNumber(),
                a.getPurchaseDate(), a.getAmcStartDate(), a.getAmcEndDate(),
                a.getNextServiceDue(), a.getNotes(), a.getCreatedAt()
        );
    }
}
