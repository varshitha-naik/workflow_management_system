package com.example.workflow_management_system.service;

import com.example.workflow_management_system.dto.RequestCreateRequest;
import com.example.workflow_management_system.dto.RequestResponse;
import com.example.workflow_management_system.dto.RequestActionResponse;
import com.example.workflow_management_system.model.*;
import com.example.workflow_management_system.repository.RequestActionRepository;
import com.example.workflow_management_system.repository.RequestRepository;
import com.example.workflow_management_system.repository.UserRepository;
import com.example.workflow_management_system.repository.WorkflowRepository;
import com.example.workflow_management_system.repository.WorkflowStepRepository;
import com.example.workflow_management_system.security.SecurityUtils;
import com.example.workflow_management_system.security.UserPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class RequestService {

    private final RequestRepository requestRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final RequestAssignmentService requestAssignmentService;
    private final RequestActionRepository requestActionRepository;
    private final AuditLogService auditLogService;

    public RequestService(RequestRepository requestRepository,
            WorkflowRepository workflowRepository,
            WorkflowStepRepository workflowStepRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            RequestAssignmentService requestAssignmentService,
            RequestActionRepository requestActionRepository,
            AuditLogService auditLogService) {
        this.requestRepository = requestRepository;
        this.workflowRepository = workflowRepository;
        this.workflowStepRepository = workflowStepRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.requestAssignmentService = requestAssignmentService;
        this.requestActionRepository = requestActionRepository;
        this.auditLogService = auditLogService;
    }

    public RequestResponse createRequest(RequestCreateRequest createRequest) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();

        Workflow workflow = workflowRepository.findById(createRequest.workflowId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        if (!workflow.getTenant().getId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Workflow belongs to another tenant");
        }

        List<WorkflowStep> steps = workflowStepRepository.findByWorkflowIdOrderByStepOrderAsc(workflow.getId());
        if (steps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow has no steps");
        }

        WorkflowStep firstStep = steps.get(0);

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String payloadJson = null;
        if (createRequest.payload() != null) {
            try {
                payloadJson = objectMapper.writeValueAsString(createRequest.payload());
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload format");
            }
        }

        Request request = new Request();
        request.setWorkflow(workflow);
        request.setCreatedBy(user);
        request.setCurrentStep(firstStep);
        request.setStatus(RequestStatus.IN_PROGRESS);
        request.setTenantId(tenantId);
        request.setPayload(payloadJson);

        Request savedRequest = requestRepository.save(request);
        requestAssignmentService.createAssignmentsForStep(savedRequest, firstStep);

        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("workflowId", workflow.getId());
        details.put("workflowName", workflow.getName());
        details.put("payload", createRequest.payload());
        auditLogService.logEvent("REQUEST", String.valueOf(savedRequest.getId()), "REQUEST_CREATED", details);

        return mapToResponse(savedRequest);
    }

    @Transactional(readOnly = true)
    public RequestResponse getRequest(Long id) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Request request = requestRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        return mapToResponse(request);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<RequestResponse> getAllRequests(
            RequestStatus status, Long workflowId, Long createdByUserId,
            LocalDateTime fromDate, LocalDateTime toDate,
            org.springframework.data.domain.Pageable pageable) {
        Long tenantId = SecurityUtils.getCurrentTenantId();

        org.springframework.data.jpa.domain.Specification<Request> spec = com.example.workflow_management_system.specification.RequestSpecification
                .filterRequests(
                        tenantId, status, workflowId, createdByUserId, fromDate, toDate);

        return requestRepository.findAll(spec, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<RequestResponse> getMyRequests(
            org.springframework.data.domain.Pageable pageable) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return requestRepository.findByTenantIdAndCreatedBy_Id(tenantId, currentUser.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<RequestResponse> getPendingApprovals() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        // Determine role from principal. Assuming 1:1 role mapping
        UserRole role = UserRole.valueOf(currentUser.getRole());

        return requestRepository
                .findByTenantIdAndStatusAndCurrentStep_RequiredRole(tenantId, RequestStatus.IN_PROGRESS, role).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void approveRequest(Long requestId, String comment) {
        processAction(requestId, comment, ActionType.APPROVE);
    }

    public void rejectRequest(Long requestId, String comment) {
        processAction(requestId, comment, ActionType.REJECT);
    }

    private void processAction(Long requestId, String comment, ActionType actionType) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.getId()).orElseThrow();

        Request request = requestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is not in progress");
        }

        WorkflowStep currentStep = request.getCurrentStep();

        // Validate Role (skip check if AUTO_APPROVE system action, but here we confuse
        // SYSTEM with User Action for now)
        // Ideally we separate system auto-approve. For USER action, check role.
        if (actionType != ActionType.AUTO_APPROVE) {
            if (!currentUser.getRole().equals(currentStep.getRequiredRole().name())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You do not have the required role to approve this step.");
            }
            // Mark assignment complete
            requestAssignmentService.completeAssignment(request, user);
        }

        // Record Action
        RequestAction action = new RequestAction(request, user, actionType, currentStep, null, comment, tenantId);

        if (actionType == ActionType.REJECT) {
            request.setStatus(RequestStatus.REJECTED);
            requestActionRepository.save(action);
            requestRepository.save(request);
            return;
        }

        // Handle APPROVE / AUTO_APPROVE
        List<WorkflowStep> allSteps = workflowStepRepository
                .findByWorkflowIdOrderByStepOrderAsc(request.getWorkflow().getId());
        WorkflowStep nextStep = allSteps.stream()
                .filter(s -> s.getStepOrder() > currentStep.getStepOrder())
                .findFirst()
                .orElse(null);

        action.setToStep(nextStep); // Can be null if finished
        requestActionRepository.save(action);

        if (nextStep == null) {
            request.setStatus(RequestStatus.COMPLETED);
            request.setCurrentStep(currentStep); // Stay on last step visually or handle differently? Standard is stay.
        } else {
            request.setCurrentStep(nextStep);
            // Create new Assignments
            requestAssignmentService.createAssignmentsForStep(request, nextStep);

            // Check Auto-Approve
            if (nextStep.isAutoApprove()) {
                // Trigger system auto approve
                // Recursive or separate call?
                // We need to simulate System User action or similar.
                // For now, let's just log it as the SAME user but with action Auto-Approve to
                // keep it simple,
                // OR we leave it for a background job. USER REQUEST says: "system automatically
                // processes".
                // Since we are in a transaction, we can just proceed.
                processAction(requestId, "System Auto-Approved", ActionType.AUTO_APPROVE);
            }
        }
        requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<RequestActionResponse> getRequestHistory(Long requestId) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return requestActionRepository.findByRequestIdAndTenantIdOrderByActionTimeAsc(requestId, tenantId)
                .stream()
                .map(action -> new RequestActionResponse(
                        action.getId(),
                        action.getRequest().getId(),
                        action.getActionBy().getId(),
                        action.getActionBy().getUsername(),
                        action.getActionType(),
                        action.getFromStep().getId(),
                        action.getFromStep().getStepName(),
                        action.getToStep() != null ? action.getToStep().getId() : null,
                        action.getToStep() != null ? action.getToStep().getStepName() : null,
                        action.getComments(),
                        action.getActionTime()))
                .collect(Collectors.toList());
    }

    private RequestResponse mapToResponse(Request request) {
        Map<String, Object> payloadMap = null;
        try {
            if (request.getPayload() != null) {
                payloadMap = objectMapper.readValue(request.getPayload(), Map.class);
            }
        } catch (JsonProcessingException e) {
            // Log error or handle gracefully
        }

        return new RequestResponse(
                request.getId(),
                request.getWorkflow().getId(),
                request.getWorkflow().getName(),
                request.getCreatedBy().getId(),
                request.getCreatedBy().getUsername(),
                request.getCurrentStep().getId(),
                request.getCurrentStep().getStepName(),
                request.getStatus(),
                payloadMap,
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
