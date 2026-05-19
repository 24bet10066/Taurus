package com.serviceos.technician.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "parts-service", url = "${services.parts.url}")
public interface PartsServiceClient {

    @PutMapping("/internal/credit/{technicianId}/limit")
    void updateCreditLimit(@PathVariable("technicianId") UUID technicianId,
                           @RequestParam("limit") BigDecimal limit);
}
