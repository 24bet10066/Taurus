package com.serviceos.job.controller;

import com.serviceos.job.dto.request.*;
import com.serviceos.job.dto.response.InvoiceDTO;
import com.serviceos.job.dto.response.JobResponse;
import com.serviceos.job.dto.response.PublicBookingResponse;
import com.serviceos.job.security.AuthenticatedUser;
import com.serviceos.job.service.InvoiceService;
import com.serviceos.job.service.JobFSM;
import com.serviceos.job.service.JobService;
import com.serviceos.shared.dto.ApiResponse;
import com.serviceos.shared.dto.PageResponse;
import com.serviceos.shared.enums.JobStatus;
import com.serviceos.shared.result.JobTransitionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@Tag(name = "Jobs", description = "Job lifecycle management")
public class JobController {

    private final JobService jobService;
    private final JobFSM jobFSM;
    private final InvoiceService invoiceService;

    public JobController(JobService jobService, JobFSM jobFSM, InvoiceService invoiceService) {
        this.jobService = jobService;
        this.jobFSM = jobFSM;
        this.invoiceService = invoiceService;
    }

    // -------------------------------------------------------------------------
    // Public endpoint — no auth, rate-limited
    // -------------------------------------------------------------------------

    @PostMapping("/public")
    @Operation(summary = "Customer self-booking (no auth required)")
    public ResponseEntity<ApiResponse<PublicBookingResponse>> publicBooking(
            @RequestBody @Valid PublicBookingRequest req) {
        PublicBookingResponse response = jobService.publicBooking(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, response.message()));
    }

    // -------------------------------------------------------------------------
    // Authenticated endpoints
    // -------------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    @Operation(summary = "Create a new job")
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @RequestBody @Valid CreateJobRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        JobResponse job = jobService.createJob(req, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(job, "Job created"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all jobs with optional filters")
    public ResponseEntity<ApiResponse<PageResponse<JobResponse>>> listJobs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String customerPhone,
            @RequestParam(required = false) UUID technicianId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<JobResponse> result = jobService.listJobs(
                status, area, date, customerPhone, technicianId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TECHNICIAN_HIRED')")
    @Operation(summary = "Technician: view own assigned jobs")
    public ResponseEntity<ApiResponse<List<JobResponse>>> myJobs(
            @AuthenticationPrincipal AuthenticatedUser user) {
        List<JobResponse> jobs = jobService.getMyJobs(user.userId());
        return ResponseEntity.ok(ApiResponse.ok(jobs));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN_HIRED')")
    @Operation(summary = "Get job detail")
    public ResponseEntity<ApiResponse<JobResponse>> getJob(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        JobResponse job = jobService.getJob(id, user);
        return ResponseEntity.ok(ApiResponse.ok(job));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN_HIRED')")
    @Operation(summary = "Transition job status (FSM-guarded)")
    public ResponseEntity<ApiResponse<?>> transitionStatus(
            @PathVariable UUID id,
            @RequestBody @Valid StatusTransitionRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        JobTransitionResult result = jobFSM.transition(id, req.status(), user.userId(), req.reason());
        return switch (result) {
            case JobTransitionResult.Success s ->
                    ResponseEntity.ok(ApiResponse.ok(s, "Status updated to " + s.to()));
            case JobTransitionResult.InvalidTransition it ->
                    ResponseEntity.unprocessableEntity()
                            .body(ApiResponse.fail("Invalid transition " + it.from() + " → " + it.to() + ": " + it.reason()));
            case JobTransitionResult.JobNotFound jnf ->
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.fail("Job not found: " + jnf.jobId()));
            case JobTransitionResult.TerminalState ts ->
                    ResponseEntity.unprocessableEntity()
                            .body(ApiResponse.fail("Job is already in terminal state: " + ts.current()));
            case JobTransitionResult.Unauthorized u ->
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.fail(u.reason()));
        };
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually assign primary and optional assistant technician")
    public ResponseEntity<ApiResponse<JobResponse>> assignJob(
            @PathVariable UUID id,
            @RequestBody @Valid AssignJobRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        JobResponse job = jobService.assignJob(id, req, user.userId());
        return ResponseEntity.ok(ApiResponse.ok(job, "Technician(s) assigned"));
    }

    @PostMapping("/{id}/auto-assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Trigger smart auto-assignment via scoring algorithm")
    public ResponseEntity<ApiResponse<String>> autoAssign(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Optional<UUID> techId = jobService.autoAssign(id, user.userId());
        if (techId.isPresent()) {
            return ResponseEntity.ok(ApiResponse.ok(techId.get().toString(), "Auto-assigned to technician"));
        }
        return ResponseEntity.ok(ApiResponse.fail("No suitable technician found; assign manually"));
    }

    @PostMapping("/{id}/parts")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN_HIRED')")
    @Operation(summary = "Add a part used in this job")
    public ResponseEntity<ApiResponse<JobResponse>> addPart(
            @PathVariable UUID id,
            @RequestBody @Valid AddPartRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        JobResponse job = jobService.addPart(id, req, user.userId());
        return ResponseEntity.ok(ApiResponse.ok(job, "Part added"));
    }

    @GetMapping("/{id}/invoice")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate invoice for a completed job")
    public ResponseEntity<ApiResponse<InvoiceDTO>> getInvoice(@PathVariable UUID id) {
        InvoiceDTO invoice = invoiceService.generateInvoice(id);
        return ResponseEntity.ok(ApiResponse.ok(invoice));
    }

    @PostMapping("/{id}/payment")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN_HIRED')")
    @Operation(summary = "Confirm payment collected")
    public ResponseEntity<ApiResponse<JobResponse>> confirmPayment(
            @PathVariable UUID id,
            @RequestBody @Valid PaymentRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        JobResponse job = jobService.confirmPayment(id, req);
        return ResponseEntity.ok(ApiResponse.ok(job, "Payment recorded"));
    }
}
