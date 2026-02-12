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
    private final com.example.workflow_management_system.repository.WorkflowStepRepository workflowStepRepository;

    public WorkflowActionService(WorkflowActionRepository workflowActionRepository,
            WorkflowRepository workflowRepository,
            com.example.workflow_management_system.repository.WorkflowStepRepository workflowStepRepository) {
        this.workflowActionRepository = workflowActionRepository;
        this.workflowRepository = workflowRepository;
        this.workflowStepRepository = workflowStepRepository;
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
        com.example.workflow_management_system.security.UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        String currentRole = currentUser.getRole(); // Role from token

        // Requirement: "Any workflow whose FIRST step requiredRole = [Role] must
        // appear"
        // Instead of fetching "WorkflowActions" (which might not exist), we fetch
        // active Workflows directly.

        List<Workflow> startableWorkflows = workflowRepository.findByTenantId(tenantId).stream()
                .filter(Workflow::isActive)
                .filter(workflow -> canStartWorkflow(workflow, currentRole))
                .collect(Collectors.toList());

        return startableWorkflows.stream()
                .map(workflow -> new WorkflowActionResponse(
                        workflow.getId(), // Use Workflow ID as the "Action ID" for the dropdown
                        workflow.getName(), // Use Workflow Name
                        workflow.getId(),
                        workflow.getName(),
                        workflow.isActive(),
                        workflow.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private boolean canStartWorkflow(Workflow workflow, String currentRole) {
        // Find the first step (stepOrder = 1)
        return workflowStepRepository.findByWorkflowIdOrderByStepOrderAsc(workflow.getId()).stream()
                .filter(step -> step.getStepOrder() == 1)
                .findFirst()
                .map(firstStep -> {
                    com.example.workflow_management_system.model.UserRole requiredRole = firstStep.getRequiredRole();
                    // Simple Exact Match Logic as per requirement
                    // USER -> USER
                    // TENANT_MANAGER -> TENANT_MANAGER
                    // TENANT_ADMIN -> TENANT_ADMIN

                    // ADMINs can typically see everything, but requirement says "User should NOT
                    // see Manager workflows"
                    // and implied exact match for visibility.
                    // However, let's stick to the prompt:
                    // "USER role should ONLY see workflows where first_step.required_role == USER"
                    // "Manager cannot create request even when workflow starts with TENANT_MANAGER"

                    if ("GLOBAL_ADMIN".equals(currentRole))
                        return true; // Super Admin sees all

                    return requiredRole.name().equals(currentRole);
                })
                .orElse(false); // If no first step, nobody can start it (effectively)
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
