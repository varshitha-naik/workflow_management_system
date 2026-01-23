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
    public ResponseEntity<List<WorkflowStepResponse>> getStepsByWorkflow(@PathVariable Long workflowId) {
        return ResponseEntity.ok(workflowStepService.getStepsByWorkflow(workflowId));
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
