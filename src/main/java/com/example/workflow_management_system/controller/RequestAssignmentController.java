package com.example.workflow_management_system.controller;

import com.example.workflow_management_system.dto.RequestAssignmentResponse;
import com.example.workflow_management_system.service.RequestAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RequestAssignmentController {

    private final RequestAssignmentService requestAssignmentService;

    public RequestAssignmentController(RequestAssignmentService requestAssignmentService) {
        this.requestAssignmentService = requestAssignmentService;
    }

    @GetMapping("/assignments/my")
    public ResponseEntity<List<RequestAssignmentResponse>> getMyAssignments() {
        return ResponseEntity.ok(requestAssignmentService.getMyAssignments());
    }

    @GetMapping("/requests/{id}/assignments")
    public ResponseEntity<List<RequestAssignmentResponse>> getAssignmentsByRequest(@PathVariable Long id) {
        return ResponseEntity.ok(requestAssignmentService.getAssignmentsByRequest(id));
    }
}
