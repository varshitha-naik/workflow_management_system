package com.example.workflow_management_system.controller;

import com.example.workflow_management_system.dto.WorkflowStepRequest;
import com.example.workflow_management_system.dto.WorkflowStepResponse;
import com.example.workflow_management_system.service.WorkflowStepService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class WorkflowStepController {

    private final WorkflowStepService workflowStepService;

    public WorkflowStepController(WorkflowStepService workflowStepService) {
        this.workflowStepService = workflowStepService;
    }

    @PostMapping("/workflows/{workflowId}/steps")
    public ResponseEntity<WorkflowStepResponse> createStep(
            @PathVariable Long workflowId,
            @Valid @RequestBody WorkflowStepRequest request) {
        return ResponseEntity.ok(workflowStepService.createStep(workflowId, request));
    }

    @GetMapping("/workflows/{workflowId}/steps")
    public ResponseEntity<org.springframework.data.domain.Page<WorkflowStepResponse>> getStepsByWorkflow(
            @PathVariable Long workflowId,
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(workflowStepService.getStepsByWorkflow(workflowId, pageable));
    }

    @GetMapping("/workflow-steps/{id}")
    public ResponseEntity<WorkflowStepResponse> getStepById(@PathVariable Long id) {
        return ResponseEntity.ok(workflowStepService.getStepById(id));
    }

    @PutMapping("/workflow-steps/{id}")
    public ResponseEntity<WorkflowStepResponse> updateStep(
            @PathVariable Long id,
            @Valid @RequestBody WorkflowStepRequest request) {
        return ResponseEntity.ok(workflowStepService.updateStep(id, request));
    }

    @DeleteMapping("/workflow-steps/{id}")
    public ResponseEntity<Void> deleteStep(@PathVariable Long id) {
        workflowStepService.deleteStep(id);
        return ResponseEntity.ok().build();
    }
}
