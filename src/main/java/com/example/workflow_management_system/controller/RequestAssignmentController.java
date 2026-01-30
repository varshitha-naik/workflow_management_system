package com.example.workflow_management_system.controller;

import com.example.workflow_management_system.dto.RequestAssignmentResponse;
import com.example.workflow_management_system.service.RequestAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Assignments", description = "Request Assignment endpoints")
public class RequestAssignmentController {

    private final RequestAssignmentService requestAssignmentService;

    public RequestAssignmentController(RequestAssignmentService requestAssignmentService) {
        this.requestAssignmentService = requestAssignmentService;
    }

    @GetMapping("/assignments/my")
    public ResponseEntity<org.springframework.data.domain.Page<RequestAssignmentResponse>> getMyAssignments(
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(requestAssignmentService.getMyAssignments(pageable));
    }

    @GetMapping("/requests/{id}/assignments")
    public ResponseEntity<List<RequestAssignmentResponse>> getAssignmentsByRequest(@PathVariable Long id) {
        return ResponseEntity.ok(requestAssignmentService.getAssignmentsByRequest(id));
    }
}
