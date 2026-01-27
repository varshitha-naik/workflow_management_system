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
    public ResponseEntity<List<RequestResponse>> getAllRequests() {
        List<RequestResponse> requests = requestService.getAllRequests();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequestResponse> getRequest(@PathVariable Long id) {
        RequestResponse request = requestService.getRequest(id);
        return ResponseEntity.ok(request);
    }

    @GetMapping("/my")
    public ResponseEntity<List<RequestResponse>> getMyRequests() {
        return ResponseEntity.ok(requestService.getMyRequests());
    }

    @GetMapping("/approvals")
    public ResponseEntity<List<RequestResponse>> getPendingApprovals() {
        return ResponseEntity.ok(requestService.getPendingApprovals());
    }

    @GetMapping("/assignments")
    public ResponseEntity<List<com.example.workflow_management_system.dto.RequestAssignmentResponse>> getMyAssignments() {
        return ResponseEntity.ok(requestAssignmentService.getMyAssignments());
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
