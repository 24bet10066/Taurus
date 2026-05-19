package com.serviceos.job.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "technician-service", url = "${services.technician.url}")
public interface TechnicianClient {

    @GetMapping("/internal/technicians/available")
    List<AvailableTechnicianDTO> getAvailableTechnicians(@RequestParam("applianceType") String applianceType);

    @GetMapping("/internal/technicians/{id}/trust-score")
    TrustScoreDTO getTrustScore(@PathVariable("id") UUID technicianId);

    @PutMapping("/internal/technicians/{id}/active-jobs")
    void updateActiveJobCount(@PathVariable("id") UUID technicianId, @RequestParam("delta") int delta);

    @GetMapping("/internal/technicians/{id}")
    TechnicianDetailDTO getTechnicianDetail(@PathVariable("id") UUID technicianId);
}
