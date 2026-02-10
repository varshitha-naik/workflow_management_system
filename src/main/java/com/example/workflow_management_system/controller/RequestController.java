package com.example.workflow_management_system.controller;

import com.example.workflow_management_system.dto.RequestCreateRequest;
import com.example.workflow_management_system.dto.RequestResponse;
import com.example.workflow_management_system.service.RequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/requests")
@Validated
@Tag(name = "Requests", description = "Request management endpoints")
public class RequestController {

    private final RequestService requestService;
    private final com.example.workflow_management_system.service.RequestAssignmentService requestAssignmentService;
    private final com.example.workflow_management_system.service.ExportService exportService;

    public RequestController(RequestService requestService,
            com.example.workflow_management_system.service.RequestAssignmentService requestAssignmentService,
            com.example.workflow_management_system.service.ExportService exportService) {
        this.requestService = requestService;
        this.requestAssignmentService = requestAssignmentService;
        this.exportService = exportService;
    }

    @GetMapping("/export")
    public void exportRequests(
            @RequestParam(required = false) com.example.workflow_management_system.model.RequestStatus status,
            @RequestParam(required = false) Long workflowId,
            @RequestParam(required = false) Long createdByUserId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime toDate,
            @RequestParam(defaultValue = "csv") String format,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {

        com.example.workflow_management_system.service.ExportService.ExportFormat exportFormat;
        try {
            exportFormat = com.example.workflow_management_system.service.ExportService.ExportFormat
                    .valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid format. Supported: csv, xlsx");
            return;
        }

        response.setContentType(
                exportFormat == com.example.workflow_management_system.service.ExportService.ExportFormat.CSV
                        ? "text/csv"
                        : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String extension = exportFormat == com.example.workflow_management_system.service.ExportService.ExportFormat.CSV
                ? "csv"
                : "xlsx";
        response.setHeader("Content-Disposition",
                "attachment; filename=\"requests_" + java.time.LocalDateTime.now() + "." + extension + "\"");

        java.util.List<RequestResponse> requests;
        com.example.workflow_management_system.security.UserPrincipal currentUser = com.example.workflow_management_system.security.SecurityUtils
                .getCurrentUser();

        if (currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TENANT_MANAGER")
                        || a.getAuthority().equals("ROLE_TENANT_ADMIN")
                        || a.getAuthority().equals("ROLE_GLOBAL_ADMIN"))) {
            requests = requestService.getAllRequestsForExport(status, workflowId, createdByUserId, fromDate, toDate);
        } else {
            // Users export their own requests with applied filters
            requests = requestService.getAllRequestsForExport(status, workflowId, currentUser.getId(), fromDate,
                    toDate);
        }

        exportService.exportRequests(requests, exportFormat, response.getOutputStream());
    }

    @PostMapping
    @Operation(summary = "Create Request", description = "Creates a new request given a workflow ID and payload.")
    public ResponseEntity<RequestResponse> createRequest(@RequestBody RequestCreateRequest request) {
        RequestResponse createdRequest = requestService.createRequest(request);
        return new ResponseEntity<>(createdRequest, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<RequestResponse>> getAllRequests(
            @RequestParam(required = false) com.example.workflow_management_system.model.RequestStatus status,
            @RequestParam(required = false) Long workflowId,
            @RequestParam(required = false) Long createdByUserId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime toDate,
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {

        java.util.Set<String> allowedFields = java.util.Set.of("id", "createdAt", "updatedAt", "status");
        org.springframework.data.domain.Sort defaultSort = org.springframework.data.domain.Sort
                .by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt");

        pageable = com.example.workflow_management_system.util.PaginationUtils.validateAndApplyDefaults(pageable,
                allowedFields, defaultSort);

        org.springframework.data.domain.Page<RequestResponse> requests = requestService.getAllRequests(status,
                workflowId, createdByUserId, fromDate, toDate, pageable);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Request Details", description = "Retrieves a request by its ID.")
    public ResponseEntity<RequestResponse> getRequest(@PathVariable @Min(1) Long id) {
        RequestResponse request = requestService.getRequest(id);
        return ResponseEntity.ok(request);
    }

    @GetMapping("/my")
    public ResponseEntity<org.springframework.data.domain.Page<RequestResponse>> getMyRequests(
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(requestService.getMyRequests(pageable));
    }

    @GetMapping("/approvals")
    public ResponseEntity<List<RequestResponse>> getPendingApprovals() {
        return ResponseEntity.ok(requestService.getPendingApprovals());
    }

    @GetMapping("/assignments")
    public ResponseEntity<org.springframework.data.domain.Page<com.example.workflow_management_system.dto.RequestAssignmentResponse>> getMyAssignments(
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(requestAssignmentService.getMyAssignments(pageable));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveRequest(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        requestService.approveRequest(id, body.getOrDefault("comments", ""));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectRequest(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        requestService.rejectRequest(id, body.getOrDefault("comments", ""));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<com.example.workflow_management_system.dto.RequestActionResponse>> getRequestHistory(
            @PathVariable Long id) {
        return ResponseEntity.ok(requestService.getRequestHistory(id));
    }
}
