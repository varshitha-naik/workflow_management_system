package com.example.workflow_management_system.service;

import com.example.workflow_management_system.dto.RequestCreateRequest;
import com.example.workflow_management_system.dto.RequestResponse;
import com.example.workflow_management_system.dto.RequestActionResponse;
import com.example.workflow_management_system.model.*;
import com.example.workflow_management_system.security.SecurityUtils;
import com.example.workflow_management_system.security.UserPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import com.example.workflow_management_system.repository.RequestRepository;
import com.example.workflow_management_system.repository.WorkflowRepository;
import com.example.workflow_management_system.repository.WorkflowStepRepository;
import com.example.workflow_management_system.repository.UserRepository;
import com.example.workflow_management_system.repository.RequestActionRepository;

import java.time.LocalDateTime;

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
    private final MeterRegistry meterRegistry;
    private final NotificationService notificationService;

    private final Counter requestCreatedCounter;
    private final Counter requestApprovedCounter;
    private final Counter requestRejectedCounter;

    public RequestService(RequestRepository requestRepository,
            WorkflowRepository workflowRepository,
            WorkflowStepRepository workflowStepRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            RequestAssignmentService requestAssignmentService,
            RequestActionRepository requestActionRepository,
            AuditLogService auditLogService,
            MeterRegistry meterRegistry,
            NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.workflowRepository = workflowRepository;
        this.workflowStepRepository = workflowStepRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.requestAssignmentService = requestAssignmentService;
        this.requestActionRepository = requestActionRepository;
        this.auditLogService = auditLogService;
        this.meterRegistry = meterRegistry;
        this.notificationService = notificationService;

        this.requestCreatedCounter = meterRegistry.counter("requests_created_total");
        this.requestApprovedCounter = meterRegistry.counter("requests_approved_total");
        this.requestRejectedCounter = meterRegistry.counter("requests_rejected_total");
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

        // Check for Auto-Completion of First Step
        boolean isCreatorStep = firstStep.getRequiredRole().name().equals(currentUser.getRole())
                || firstStep.getRequiredRole() == UserRole.USER;

        if (isCreatorStep || firstStep.isAutoApprove()) {
            RequestAction submitAction = new RequestAction(savedRequest, user, ActionType.APPROVE, firstStep, null,
                    "Request Submitted", tenantId);

            List<WorkflowStep> allSteps = workflowStepRepository.findByWorkflowIdOrderByStepOrderAsc(workflow.getId());
            WorkflowStep nextStep = allSteps.stream()
                    .filter(s -> s.getStepOrder() > firstStep.getStepOrder())
                    .findFirst()
                    .orElse(null);

            submitAction.setToStep(nextStep);
            requestActionRepository.save(submitAction);

            if (nextStep == null) {
                savedRequest.setStatus(RequestStatus.COMPLETED);
                notificationService.createNotification("Your request #" + savedRequest.getId() + " has been completed.",
                        "SUCCESS", savedRequest, user);
            } else {
                savedRequest.setCurrentStep(nextStep);
                requestAssignmentService.createAssignmentsForStep(savedRequest, nextStep);

                // Notify Next Approvers
                notificationService.createNotificationsForRole("New approval assigned: " + nextStep.getStepName(),
                        "ACTION", savedRequest, nextStep.getRequiredRole().name());
                // Notify User
                notificationService.createNotification("Your request moved to " + nextStep.getStepName(), "INFO",
                        savedRequest, user);

                // Handle Recursive Auto-Approve
                if (nextStep.isAutoApprove()) {
                    processAction(savedRequest.getId(), "System Auto-Approved", ActionType.AUTO_APPROVE);
                }
            }
            requestRepository.save(savedRequest);
        } else {
            // Normal flow: Create assignment for the first step
            requestAssignmentService.createAssignmentsForStep(savedRequest, firstStep);
            // Notify Approvers
            notificationService.createNotificationsForRole("New approval assigned: " + firstStep.getStepName(),
                    "ACTION", savedRequest, firstStep.getRequiredRole().name());
        }

        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("workflowId", workflow.getId());
        details.put("workflowName", workflow.getName());
        details.put("payload", createRequest.payload());
        auditLogService.logEvent("REQUEST", String.valueOf(savedRequest.getId()), "REQUEST_CREATED", details);

        requestCreatedCounter.increment();

        return mapToResponse(savedRequest);
    }

    // ... existing ...

    // START: processAction modification
    private void processAction(Long requestId, String comment, ActionType actionType) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        // For System actions, we might need a system user or use current user if
        // context exists
        User user = userRepository.findById(currentUser.getId()).orElseThrow();

        Request request = requestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is not in progress");
        }

        WorkflowStep currentStep = request.getCurrentStep();

        if (actionType != ActionType.AUTO_APPROVE) {
            if (!currentUser.getRole().equals(currentStep.getRequiredRole().name())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You do not have the required role to approve this step.");
            }
            requestAssignmentService.completeAssignment(request, user);
        }

        RequestAction action = new RequestAction(request, user, actionType, currentStep, null, comment, tenantId);

        if (actionType == ActionType.REJECT) {
            request.setStatus(RequestStatus.REJECTED);
            requestActionRepository.save(action);
            requestRepository.save(request);

            // Notify Creator
            notificationService.createNotification("Your request #" + request.getId() + " has been REJECTED.", "ERROR",
                    request, request.getCreatedBy());

            return;
        }

        List<WorkflowStep> allSteps = workflowStepRepository
                .findByWorkflowIdOrderByStepOrderAsc(request.getWorkflow().getId());
        WorkflowStep nextStep = allSteps.stream()
                .filter(s -> s.getStepOrder() > currentStep.getStepOrder())
                .findFirst()
                .orElse(null);

        action.setToStep(nextStep);
        requestActionRepository.save(action);

        if (nextStep == null) {
            request.setStatus(RequestStatus.COMPLETED);
            request.setCurrentStep(currentStep);

            // Notify Creator
            notificationService.createNotification("Your request #" + request.getId() + " is now COMPLETED.", "SUCCESS",
                    request, request.getCreatedBy());

        } else {
            request.setCurrentStep(nextStep);
            requestAssignmentService.createAssignmentsForStep(request, nextStep);

            // Notify Next Approvers
            notificationService.createNotificationsForRole("New approval assigned: " + nextStep.getStepName(), "ACTION",
                    request, nextStep.getRequiredRole().name());

            // Notify Creator (if creators are interested in progress)
            notificationService.createNotification(
                    "Your request #" + request.getId() + " moved to " + nextStep.getStepName(), "INFO", request,
                    request.getCreatedBy());

            if (nextStep.isAutoApprove()) {
                processAction(requestId, "System Auto-Approved", ActionType.AUTO_APPROVE);
            }
        }
        requestRepository.save(request);
    }
    // END: processAction modification

    // ... remainders ...

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
    public List<RequestResponse> getAllRequestsForExport(
            RequestStatus status, Long workflowId, Long createdByUserId,
            LocalDateTime fromDate, LocalDateTime toDate) {
        Long tenantId = SecurityUtils.getCurrentTenantId();

        org.springframework.data.jpa.domain.Specification<Request> spec = com.example.workflow_management_system.specification.RequestSpecification
                .filterRequests(
                        tenantId, status, workflowId, createdByUserId, fromDate, toDate);

        return requestRepository.findAll(spec).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
        requestApprovedCounter.increment();
    }

    public void rejectRequest(Long requestId, String comment) {
        processAction(requestId, comment, ActionType.REJECT);
        requestRejectedCounter.increment();
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

    public RequestResponse mapToResponse(Request request) {
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
                request.getCurrentStep().getRequiredRole().name(),
                request.getStatus(),
                payloadMap,
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
