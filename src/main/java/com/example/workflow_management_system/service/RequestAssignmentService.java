package com.example.workflow_management_system.service;

import com.example.workflow_management_system.dto.RequestAssignmentResponse;
import com.example.workflow_management_system.model.*;
import com.example.workflow_management_system.repository.RequestAssignmentRepository;
import com.example.workflow_management_system.repository.UserRepository;
import com.example.workflow_management_system.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RequestAssignmentService {

    private final RequestAssignmentRepository requestAssignmentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public RequestAssignmentService(RequestAssignmentRepository requestAssignmentRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.requestAssignmentRepository = requestAssignmentRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public void createAssignment(Request request, User assignee) {
        if (request.getStatus() == RequestStatus.COMPLETED || request.getStatus() == RequestStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot create assignment for a terminal request (COMPLETED/REJECTED).");
        }

        RequestAssignment assignment = new RequestAssignment(
                request,
                assignee,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(2), // Default 2 days due
                AssignmentStatus.ASSIGNED,
                request.getTenantId());
        RequestAssignment saved = requestAssignmentRepository.save(assignment);

        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("requestId", saved.getRequest().getId());
        details.put("assignedTo", saved.getAssignedTo().getUsername());
        details.put("status", saved.getStatus());

        // System performs this usually via trigger, or user trigger?
        // createAssignment is often called by system.
        // logEvent(entityType, entityId, action, details) uses SecurityContext if
        // available.
        // But if called by RequestService (user action), it works.
        auditLogService.logEvent("REQUEST_ASSIGNMENT", String.valueOf(saved.getId()), "ASSIGNMENT_CREATED", details);
    }

    public void completeAssignment(Request request, User user) {
        // Find active assignment for this request and user
        // If found, mark completed.
        // Strict tenant check via repository
        List<RequestAssignment> assignments = requestAssignmentRepository
                .findByAssignedTo_IdAndTenantIdOrderByAssignedAtDesc(user.getId(), request.getTenantId());

        for (RequestAssignment assignment : assignments) {
            if (assignment.getRequest().getId().equals(request.getId())
                    && assignment.getStatus() == AssignmentStatus.ASSIGNED) {
                assignment.setStatus(AssignmentStatus.COMPLETED);
                RequestAssignment saved = requestAssignmentRepository.save(assignment);

                java.util.Map<String, Object> details = new java.util.HashMap<>();
                details.put("requestId", saved.getRequest().getId());
                details.put("assignedTo", saved.getAssignedTo().getUsername());
                details.put("status", saved.getStatus());

                auditLogService.logEvent("REQUEST_ASSIGNMENT", String.valueOf(saved.getId()), "ASSIGNMENT_COMPLETED",
                        details);
                // We assume only one active assignment per user per request usually
                break;
            }
        }
    }

    public void createAssignmentsForStep(Request request, WorkflowStep step) {
        if (request.getStatus() == RequestStatus.COMPLETED || request.getStatus() == RequestStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot create assignment for a terminal request (COMPLETED/REJECTED).");
        }

        // Find all users in tenant with required role
        List<User> eligibleUsers = userRepository.findByTenant_Id(request.getTenantId()).stream()
                .filter(u -> u.getRole() == step.getRequiredRole())
                .collect(Collectors.toList());

        for (User user : eligibleUsers) {
            createAssignment(request, user);
        }
    }

    @Transactional
    public void markOverdueAssignments() {
        // Find all assigned assignments past their due date
        // Logic: status = ASSIGNED AND dueAt < now()
        // Tenant isolation: Implied as we query all and data is physically separated by
        // tenant_id column, so safe to process.
        List<RequestAssignment> overdueAssignments = requestAssignmentRepository
                .findAllByStatusAndDueAtBefore(AssignmentStatus.ASSIGNED, LocalDateTime.now());

        for (RequestAssignment assignment : overdueAssignments) {
            assignment.setStatus(AssignmentStatus.OVERDUE);
            RequestAssignment saved = requestAssignmentRepository.save(assignment);

            java.util.Map<String, Object> details = new java.util.HashMap<>();
            details.put("requestId", saved.getRequest().getId());
            details.put("assignedTo", saved.getAssignedTo().getUsername());
            details.put("status", saved.getStatus());

            // Performed by SYSTEM (null user context probably, or explicit system method)
            auditLogService.logEvent("REQUEST_ASSIGNMENT", String.valueOf(saved.getId()), "ASSIGNMENT_OVERDUE",
                    "SYSTEM", saved.getTenantId(), details);
        }
    }

    @Transactional(readOnly = true)
    public List<RequestAssignmentResponse> getMyAssignments() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long userId = SecurityUtils.getCurrentUser().getId();

        return requestAssignmentRepository.findByAssignedTo_IdAndTenantIdOrderByAssignedAtDesc(userId, tenantId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RequestAssignmentResponse> getAssignmentsByRequest(Long requestId) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return requestAssignmentRepository.findByRequestIdAndTenantIdOrderByAssignedAtAsc(requestId, tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private RequestAssignmentResponse mapToResponse(RequestAssignment assignment) {
        return new RequestAssignmentResponse(
                assignment.getId(),
                assignment.getRequest().getId(),
                assignment.getAssignedTo().getId(),
                assignment.getAssignedTo().getUsername(),
                assignment.getAssignedAt(),
                assignment.getDueAt(),
                assignment.getStatus());
    }
}
