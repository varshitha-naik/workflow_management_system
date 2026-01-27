package com.example.workflow_management_system.service;

import com.example.workflow_management_system.dto.RequestActionCreateRequest;
import com.example.workflow_management_system.dto.RequestActionResponse;
import com.example.workflow_management_system.model.*;
import com.example.workflow_management_system.repository.*;
import com.example.workflow_management_system.security.SecurityUtils;
import com.example.workflow_management_system.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class RequestActionService {

    private final RequestActionRepository requestActionRepository;
    private final RequestRepository requestRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final RequestAssignmentService requestAssignmentService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public RequestActionService(RequestActionRepository requestActionRepository,
            RequestRepository requestRepository,
            WorkflowStepRepository workflowStepRepository,
            UserRepository userRepository,
            RequestAssignmentService requestAssignmentService,
            AuditLogService auditLogService) {
        this.requestActionRepository = requestActionRepository;
        this.requestRepository = requestRepository;
        this.workflowStepRepository = workflowStepRepository;
        this.userRepository = userRepository;
        this.requestAssignmentService = requestAssignmentService;
        this.auditLogService = auditLogService;
    }

    public RequestActionResponse createAction(Long requestId, RequestActionCreateRequest createRequest) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();

        Request request = requestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is not in progress");
        }

        WorkflowStep currentStep = request.getCurrentStep();
        if (currentStep == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Current step is null");
        }

        if (createRequest.actionType() != ActionType.AUTO_APPROVE) {
            if (!hasPermission(currentUser.getRole(), currentStep.getRequiredRole())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions for this step");
            }
        }

        User actionByUser = null;
        if (createRequest.actionType() != ActionType.AUTO_APPROVE) {
            actionByUser = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            try {
                requestAssignmentService.completeAssignment(request, actionByUser);
            } catch (Exception e) {
                // Log warning but proceed? Or fail?
                // Proceed as assignment might not exist if it was role-based purely or claimed.
            }
        }

        RequestAction action = new RequestAction();
        action.setRequest(request);
        action.setActionBy(actionByUser);
        action.setActionType(createRequest.actionType());
        action.setFromStep(currentStep);
        action.setComments(createRequest.comments());
        action.setTenantId(tenantId);
        action.setActionTime(LocalDateTime.now());

        WorkflowStep nextStep = null;

        if (createRequest.actionType() == ActionType.APPROVE || createRequest.actionType() == ActionType.AUTO_APPROVE) {
            Optional<WorkflowStep> nextStepOpt = workflowStepRepository
                    .findFirstByWorkflowIdAndStepOrderGreaterThanOrderByStepOrderAsc(
                            request.getWorkflow().getId(), currentStep.getStepOrder());

            if (nextStepOpt.isPresent()) {
                nextStep = nextStepOpt.get();
                request.setCurrentStep(nextStep);
                requestAssignmentService.createAssignmentsForStep(request, nextStep);
            } else {
                request.setStatus(RequestStatus.COMPLETED);
            }
        } else if (createRequest.actionType() == ActionType.REJECT) {
            request.setStatus(RequestStatus.REJECTED);
        }

        action.setToStep(nextStep);

        RequestAction savedAction = requestActionRepository.save(action);
        requestRepository.save(request);

        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("requestId", request.getId());
        details.put("actionType", savedAction.getActionType());
        details.put("fromStep", savedAction.getFromStep().getStepName());
        if (savedAction.getToStep() != null) {
            details.put("toStep", savedAction.getToStep().getStepName());
        }
        auditLogService.logEvent("REQUEST_ACTION", String.valueOf(savedAction.getId()), "REQUEST_ACTION_CREATED",
                details);

        return mapToResponse(savedAction);
    }

    @Transactional(readOnly = true)
    public List<RequestActionResponse> getActions(Long requestId) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        // exist check
        if (!requestRepository.existsById(requestId)) { // optimization: check by ID only first or directly check tenant
            // To be secure, must check tenant
            requestRepository.findByIdAndTenantId(requestId, tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        }

        List<RequestAction> actions = requestActionRepository.findByRequestIdAndTenantIdOrderByActionTimeAsc(requestId,
                tenantId);
        return actions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private boolean hasPermission(String userRoleStr, UserRole requiredRole) {
        // Hierarchy: SUPER_ADMIN > ADMIN > USER
        int userLevel = getRoleLevel(UserRole.valueOf(userRoleStr));
        int requiredLevel = getRoleLevel(requiredRole);
        return userLevel >= requiredLevel;
    }

    private int getRoleLevel(UserRole role) {
        return switch (role) {
            case SUPER_ADMIN -> 3;
            case ADMIN -> 2;
            case USER -> 1;
        };
    }

    private RequestActionResponse mapToResponse(RequestAction action) {
        return new RequestActionResponse(
                action.getId(),
                action.getRequest().getId(),
                action.getActionBy() != null ? action.getActionBy().getId() : null,
                action.getActionBy() != null ? action.getActionBy().getUsername() : null,
                action.getActionType(),
                action.getFromStep().getId(),
                action.getFromStep().getStepName(),
                action.getToStep() != null ? action.getToStep().getId() : null,
                action.getToStep() != null ? action.getToStep().getStepName() : null,
                action.getComments(),
                action.getActionTime());
    }
}
