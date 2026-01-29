package com.example.workflow_management_system.controller;

import com.example.workflow_management_system.dto.RequestCreateRequest;
import com.example.workflow_management_system.dto.RequestResponse;
import com.example.workflow_management_system.service.RequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;
    private final com.example.workflow_management_system.service.RequestAssignmentService requestAssignmentService;

    public RequestController(RequestService requestService,
            com.example.workflow_management_system.service.RequestAssignmentService requestAssignmentService) {
        this.requestService = requestService;
        this.requestAssignmentService = requestAssignmentService;
    }

    @PostMapping
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
    public ResponseEntity<RequestResponse> getRequest(@PathVariable Long id) {
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
