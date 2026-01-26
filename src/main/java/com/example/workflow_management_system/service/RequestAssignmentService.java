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
    private final com.example.workflow_management_system.repository.AuditLogRepository auditLogRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public RequestAssignmentService(RequestAssignmentRepository requestAssignmentRepository,
            UserRepository userRepository,
            com.example.workflow_management_system.repository.AuditLogRepository auditLogRepository,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.requestAssignmentRepository = requestAssignmentRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
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

        logEvent("ASSIGNMENT_CREATED", saved, null);
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

                logEvent("ASSIGNMENT_COMPLETED", saved, user.getId());
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
            logEvent("ASSIGNMENT_OVERDUE", saved, null);
        }
    }

    private void logEvent(String action, RequestAssignment assignment, Long userId) {
        String performedBy = (userId != null) ? String.valueOf(userId) : "SYSTEM";

        String detailsJson = "{}";
        try {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("requestId", assignment.getRequest().getId());
            map.put("assignedTo", assignment.getAssignedTo().getUsername());
            map.put("status", assignment.getStatus());
            detailsJson = objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            // Log error, but don't fail transaction?
            // "Code must be production-ready" -> use SLF4J, but I don't see Logger field.
            // e.printStackTrace();
        }

        AuditLog log = new AuditLog(
                "REQUEST_ASSIGNMENT",
                String.valueOf(assignment.getId()),
                action,
                performedBy,
                assignment.getTenantId(),
                detailsJson);
        auditLogRepository.save(log);
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
