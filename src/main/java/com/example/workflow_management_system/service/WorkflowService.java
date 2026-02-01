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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final TenantRepository tenantRepository;
    private final AuditLogService auditLogService;

    public WorkflowService(WorkflowRepository workflowRepository, TenantRepository tenantRepository,
            AuditLogService auditLogService) {
        this.workflowRepository = workflowRepository;
        this.tenantRepository = tenantRepository;
        this.auditLogService = auditLogService;
    }

    @Caching(evict = {
            @CacheEvict(value = "workflows", allEntries = true),
            @CacheEvict(value = "workflow_details", allEntries = true),
            @CacheEvict(value = "workflowSteps", allEntries = true)
    })
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

        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("name", savedWorkflow.getName());
        details.put("active", savedWorkflow.isActive());
        auditLogService.logEvent("WORKFLOW", String.valueOf(savedWorkflow.getId()), "WORKFLOW_CREATED", details);

        return mapToResponse(savedWorkflow);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "workflow_details", key = "T(com.example.workflow_management_system.security.SecurityUtils).getCurrentTenantId() + '-' + #id")
    public WorkflowResponse getWorkflowById(Long id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        SecurityUtils.validateTenantAccess(workflow.getTenant().getId());

        return mapToResponse(workflow);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "workflows", key = "T(com.example.workflow_management_system.security.SecurityUtils).getCurrentTenantId()")
    public List<WorkflowResponse> getAllWorkflows() {
        Long tenantId = SecurityUtils.getCurrentUser().getTenantId();

        if (tenantId == null) {
            return java.util.Collections.emptyList();
        }

        return workflowRepository.findByTenantId(tenantId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Caching(evict = {
            @CacheEvict(value = "workflows", allEntries = true),
            @CacheEvict(value = "workflow_details", allEntries = true),
            @CacheEvict(value = "workflowSteps", allEntries = true)
    })
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

        Workflow saved = workflowRepository.save(workflow);

        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("name", saved.getName());
        details.put("active", saved.isActive());
        auditLogService.logEvent("WORKFLOW", String.valueOf(saved.getId()), "WORKFLOW_UPDATED", details);

        return mapToResponse(saved);
    }

    @Caching(evict = {
            @CacheEvict(value = "workflows", allEntries = true),
            @CacheEvict(value = "workflow_details", allEntries = true),
            @CacheEvict(value = "workflowSteps", allEntries = true)
    })
    public WorkflowResponse updateStatus(Long id, boolean active) {
        checkWriteAccess();

        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        SecurityUtils.validateTenantAccess(workflow.getTenant().getId());

        workflow.setActive(active);
        Workflow saved = workflowRepository.save(workflow);

        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("active", saved.isActive());
        auditLogService.logEvent("WORKFLOW", String.valueOf(saved.getId()), "WORKFLOW_UPDATED", details);

        return mapToResponse(saved);
    }

    @Caching(evict = {
            @CacheEvict(value = "workflows", allEntries = true),
            @CacheEvict(value = "workflow_details", allEntries = true),
            @CacheEvict(value = "workflowSteps", allEntries = true)
    })
    public void deleteWorkflow(Long id) {
        checkWriteAccess();

        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        SecurityUtils.validateTenantAccess(workflow.getTenant().getId());

        workflowRepository.delete(workflow);

        auditLogService.logEvent("WORKFLOW", String.valueOf(id), "WORKFLOW_DELETED", null);
    }

    private void checkWriteAccess() {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        if ("USER".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Read-only access for USER role");
        }
    }

    public WorkflowResponse mapToResponse(Workflow workflow) {
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
