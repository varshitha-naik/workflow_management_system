package com.example.workflow_management_system.service;

import com.example.workflow_management_system.dto.WorkflowStepRequest;
import com.example.workflow_management_system.dto.WorkflowStepResponse;
import com.example.workflow_management_system.model.Workflow;
import com.example.workflow_management_system.model.WorkflowStep;
import com.example.workflow_management_system.repository.WorkflowRepository;
import com.example.workflow_management_system.repository.WorkflowStepRepository;
import com.example.workflow_management_system.security.SecurityUtils;
import com.example.workflow_management_system.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkflowStepService {

    private final WorkflowStepRepository workflowStepRepository;
    private final WorkflowRepository workflowRepository;
    private final AuditLogService auditLogService;

    public WorkflowStepService(WorkflowStepRepository workflowStepRepository, WorkflowRepository workflowRepository,
            AuditLogService auditLogService) {
        this.workflowStepRepository = workflowStepRepository;
        this.workflowRepository = workflowRepository;
        this.auditLogService = auditLogService;
    }

    public WorkflowStepResponse createStep(Long workflowId, WorkflowStepRequest request) {
        checkWriteAccess();

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        SecurityUtils.validateTenantAccess(workflow.getTenant().getId());

        if (workflowStepRepository.existsByWorkflowIdAndStepOrder(workflowId, request.stepOrder())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Step order must be unique within the workflow");
        }

        WorkflowStep step = new WorkflowStep(
                workflow,
                request.stepOrder(),
                request.stepName(),
                request.requiredRole(),
                request.autoApprove());

        WorkflowStep savedStep = workflowStepRepository.save(step);

        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("workflowId", workflowId);
        details.put("stepName", savedStep.getStepName());
        details.put("stepOrder", savedStep.getStepOrder());
        auditLogService.logEvent("WORKFLOW_STEP", String.valueOf(savedStep.getId()), "WORKFLOW_STEP_CREATED", details);

        return mapToResponse(savedStep);
    }

    @Transactional(readOnly = true)
    public List<WorkflowStepResponse> getStepsByWorkflow(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        SecurityUtils.validateTenantAccess(workflow.getTenant().getId());

        return workflowStepRepository.findByWorkflowIdOrderByStepOrderAsc(workflowId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkflowStepResponse getStepById(Long id) {
        WorkflowStep step = workflowStepRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow Step not found"));

        SecurityUtils.validateTenantAccess(step.getWorkflow().getTenant().getId());

        return mapToResponse(step);
    }

    public WorkflowStepResponse updateStep(Long id, WorkflowStepRequest request) {
        checkWriteAccess();

        WorkflowStep step = workflowStepRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow Step not found"));

        SecurityUtils.validateTenantAccess(step.getWorkflow().getTenant().getId());

        if (step.getStepOrder() != request.stepOrder() &&
                workflowStepRepository.existsByWorkflowIdAndStepOrder(step.getWorkflow().getId(),
                        request.stepOrder())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Step order must be unique within the workflow");
        }

        step.setStepOrder(request.stepOrder());
        step.setStepName(request.stepName());
        step.setRequiredRole(request.requiredRole());
        step.setAutoApprove(request.autoApprove());

        WorkflowStep savedStep = workflowStepRepository.save(step);

        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("workflowId", savedStep.getWorkflow().getId());
        details.put("stepName", savedStep.getStepName());
        details.put("stepOrder", savedStep.getStepOrder());
        auditLogService.logEvent("WORKFLOW_STEP", String.valueOf(savedStep.getId()), "WORKFLOW_STEP_UPDATED", details);

        return mapToResponse(savedStep);
    }

    public void deleteStep(Long id) {
        checkWriteAccess();

        WorkflowStep step = workflowStepRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow Step not found"));

        SecurityUtils.validateTenantAccess(step.getWorkflow().getTenant().getId());

        workflowStepRepository.delete(step);

        auditLogService.logEvent("WORKFLOW_STEP", String.valueOf(id), "WORKFLOW_STEP_DELETED", null);
    }

    private void checkWriteAccess() {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        if ("USER".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Read-only access for USER role");
        }
    }

    private WorkflowStepResponse mapToResponse(WorkflowStep step) {
        return new WorkflowStepResponse(
                step.getId(),
                step.getWorkflow().getId(),
                step.getStepOrder(),
                step.getStepName(),
                step.getRequiredRole(),
                step.isAutoApprove(),
                step.getCreatedAt());
    }
}
