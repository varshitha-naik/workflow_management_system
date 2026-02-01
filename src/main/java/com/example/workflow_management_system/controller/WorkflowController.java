package com.example.workflow_management_system.controller;

import com.example.workflow_management_system.dto.WorkflowRequest;
import com.example.workflow_management_system.dto.WorkflowResponse;
import com.example.workflow_management_system.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/workflows")
@Validated
@Tag(name = "Workflows", description = "Workflow management endpoints")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    public ResponseEntity<WorkflowResponse> createWorkflow(@Valid @RequestBody WorkflowRequest request) {
        return ResponseEntity.ok(workflowService.createWorkflow(request));
    }

    @GetMapping
    public ResponseEntity<List<WorkflowResponse>> getAllWorkflows() {
        return ResponseEntity.ok(workflowService.getAllWorkflows());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowResponse> getWorkflowById(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(workflowService.getWorkflowById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowResponse> updateWorkflow(@PathVariable @Min(1) Long id,
            @Valid @RequestBody WorkflowRequest request) {
        return ResponseEntity.ok(workflowService.updateWorkflow(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<WorkflowResponse> updateStatus(@PathVariable @Min(1) Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(workflowService.updateStatus(id, active));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable @Min(1) Long id) {
        workflowService.deleteWorkflow(id);
        return ResponseEntity.ok().build();
    }
}
