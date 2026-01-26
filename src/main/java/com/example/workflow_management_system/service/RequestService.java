package com.example.workflow_management_system.service;

import com.example.workflow_management_system.dto.RequestCreateRequest;
import com.example.workflow_management_system.dto.RequestResponse;
import com.example.workflow_management_system.model.*;
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

    public RequestService(RequestRepository requestRepository,
            WorkflowRepository workflowRepository,
            WorkflowStepRepository workflowStepRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            RequestAssignmentService requestAssignmentService) {
        this.requestRepository = requestRepository;
        this.workflowRepository = workflowRepository;
        this.workflowStepRepository = workflowStepRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.requestAssignmentService = requestAssignmentService;
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
    public List<RequestResponse> getAllRequests() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        List<Request> requests = requestRepository.findByTenantId(tenantId);
        return requests.stream()
                .map(this::mapToResponse)
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
