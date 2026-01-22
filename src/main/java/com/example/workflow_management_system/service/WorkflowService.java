package com.example.workflow_management_system.service;

import com.example.workflow_management_system.dto.WorkflowRequest;
import com.example.workflow_management_system.dto.WorkflowResponse;
import com.example.workflow_management_system.model.Tenant;
import com.example.workflow_management_system.model.Workflow;
import com.example.workflow_management_system.repository.TenantRepository;
import com.example.workflow_management_system.repository.WorkflowRepository;
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
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final TenantRepository tenantRepository;

    public WorkflowService(WorkflowRepository workflowRepository, TenantRepository tenantRepository) {
        this.workflowRepository = workflowRepository;
        this.tenantRepository = tenantRepository;
    }

    public WorkflowResponse createWorkflow(WorkflowRequest request) {
        checkWriteAccess();

        Long tenantId = SecurityUtils.getCurrentUser().getTenantId();

        // Ensure name is unique for the tenant
        if (workflowRepository.existsByNameAndTenantId(request.name(), tenantId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Workflow with this name already exists");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant not found"));

        Workflow workflow = new Workflow(
                request.name(),
                request.description(),
                tenant,
                request.active());

        Workflow savedWorkflow = workflowRepository.save(workflow);
        return mapToResponse(savedWorkflow);
    }

    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflowById(Long id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        SecurityUtils.validateTenantAccess(workflow.getTenant().getId());

        return mapToResponse(workflow);
    }

    @Transactional(readOnly = true)
    public List<WorkflowResponse> getAllWorkflows() {
        Long tenantId = SecurityUtils.getCurrentUser().getTenantId();
        // If SUPER_ADMIN without specific tenant, this might be null?
        // SecurityUtils logic usually requires tenantId except for SUPER_ADMIN access
        // check.
        // But here we need to filter list.
        // If SUPER_ADMIN, maybe they want to see ALL?
        // But prompt says "list".
        // I will assume listing for "current context tenant".
        // If SUPER_ADMIN calls this, they get workflows for their tenant (System).
        // If they want to see others, they'd use `getAllWorkflowsForTenant`?
        // Prompt says "Enforce strict tenant isolation". So showing only current tenant
        // workflows is safest.

        if (tenantId == null) {
            // Should not happen for authenticated users usually unless system user
            return List.of();
        }

        return workflowRepository.findByTenantId(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public WorkflowResponse updateWorkflow(Long id, WorkflowRequest request) {
        checkWriteAccess();

        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        SecurityUtils.validateTenantAccess(workflow.getTenant().getId());

        // Check name uniqueness if changed
        if (!workflow.getName().equals(request.name()) &&
                workflowRepository.existsByNameAndTenantId(request.name(), workflow.getTenant().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Workflow with this name already exists");
        }

        workflow.setName(request.name());
        workflow.setDescription(request.description());
        workflow.setActive(request.active());

        return mapToResponse(workflowRepository.save(workflow));
    }

    public WorkflowResponse updateStatus(Long id, boolean active) {
        checkWriteAccess();

        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        SecurityUtils.validateTenantAccess(workflow.getTenant().getId());

        workflow.setActive(active);
        return mapToResponse(workflowRepository.save(workflow));
    }

    public void deleteWorkflow(Long id) {
        checkWriteAccess();

        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        SecurityUtils.validateTenantAccess(workflow.getTenant().getId());

        workflowRepository.delete(workflow);
    }

    private void checkWriteAccess() {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        if ("USER".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Read-only access for USER role");
        }
    }

    private WorkflowResponse mapToResponse(Workflow workflow) {
        return new WorkflowResponse(
                workflow.getId(),
                workflow.getName(),
                workflow.getDescription(),
                workflow.getTenant().getId(),
                workflow.isActive(),
                workflow.getCreatedAt(),
                workflow.getUpdatedAt());
    }
}
