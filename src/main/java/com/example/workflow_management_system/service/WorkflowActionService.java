package com.example.workflow_management_system.service;

import com.example.workflow_management_system.dto.WorkflowActionRequest;
import com.example.workflow_management_system.dto.WorkflowActionResponse;
import com.example.workflow_management_system.model.Workflow;
import com.example.workflow_management_system.model.WorkflowAction;
import com.example.workflow_management_system.repository.WorkflowActionRepository;
import com.example.workflow_management_system.repository.WorkflowRepository;
import com.example.workflow_management_system.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkflowActionService {

    private final WorkflowActionRepository workflowActionRepository;
    private final WorkflowRepository workflowRepository;

    public WorkflowActionService(WorkflowActionRepository workflowActionRepository,
            WorkflowRepository workflowRepository) {
        this.workflowActionRepository = workflowActionRepository;
        this.workflowRepository = workflowRepository;
    }

    public WorkflowActionResponse createAction(WorkflowActionRequest request) {
        Long tenantId = SecurityUtils.getCurrentTenantId();

        Workflow workflow = workflowRepository.findById(request.workflowId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        if (!workflow.getTenant().getId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Workflow belongs to another tenant");
        }

        WorkflowAction action = new WorkflowAction(request.name(), workflow, tenantId);
        WorkflowAction savedAction = workflowActionRepository.save(action);

        return mapToResponse(savedAction);
    }

    @Transactional(readOnly = true)
    public List<WorkflowActionResponse> getAllActions() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return workflowActionRepository.findByTenantId(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkflowActionResponse> getActiveActions() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return workflowActionRepository.findByTenantIdAndActiveTrue(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private WorkflowActionResponse mapToResponse(WorkflowAction action) {
        return new WorkflowActionResponse(
                action.getId(),
                action.getName(),
                action.getWorkflow().getId(),
                action.getWorkflow().getName(),
                action.isActive(),
                action.getCreatedAt());
    }
}
