package com.example.workflow_management_system.controller;

import com.example.workflow_management_system.dto.WorkflowActionRequest;
import com.example.workflow_management_system.dto.WorkflowActionResponse;
import com.example.workflow_management_system.service.WorkflowActionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/request-actions") // requested URL mapping
@Validated
@Tag(name = "Workflow Actions", description = "Managing dynamic actions for workflows (renamed from RequestAction entity in prompt to avoid conflict)")
public class WorkflowActionController {

    private final WorkflowActionService workflowActionService;

    public WorkflowActionController(WorkflowActionService workflowActionService) {
        this.workflowActionService = workflowActionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('GLOBAL_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER')")
    public ResponseEntity<WorkflowActionResponse> createAction(@Valid @RequestBody WorkflowActionRequest request) {
        return new ResponseEntity<>(workflowActionService.createAction(request), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GLOBAL_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER')")
    public ResponseEntity<List<WorkflowActionResponse>> getAllActions() {
        return ResponseEntity.ok(workflowActionService.getAllActions());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('USER', 'GLOBAL_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER')") // User needs this for UI
    public ResponseEntity<List<WorkflowActionResponse>> getActiveActions() {
        return ResponseEntity.ok(workflowActionService.getActiveActions());
    }
}
