package com.example.workflow_management_system.service;

import com.example.workflow_management_system.dto.RequestResponse;
import com.example.workflow_management_system.dto.SearchResponse;
import com.example.workflow_management_system.dto.UserResponse;
import com.example.workflow_management_system.dto.WorkflowResponse;
import com.example.workflow_management_system.model.Request;
import com.example.workflow_management_system.model.SearchType;
import com.example.workflow_management_system.model.User;
import com.example.workflow_management_system.model.Workflow;
import com.example.workflow_management_system.repository.RequestRepository;
import com.example.workflow_management_system.repository.UserRepository;
import com.example.workflow_management_system.repository.WorkflowRepository;
import com.example.workflow_management_system.security.SecurityUtils;
import com.example.workflow_management_system.specification.RequestSpecification;
import com.example.workflow_management_system.specification.UserSpecification;
import com.example.workflow_management_system.specification.WorkflowSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class SearchService {

    private final RequestRepository requestRepository;
    private final WorkflowRepository workflowRepository;
    private final UserRepository userRepository;
    private final RequestService requestService; // To reuse mapper
    private final WorkflowService workflowService; // To reuse mapper
    private final UserService userService; // To reuse mapper

    public SearchService(RequestRepository requestRepository,
            WorkflowRepository workflowRepository,
            UserRepository userRepository,
            RequestService requestService,
            WorkflowService workflowService,
            UserService userService) {
        this.requestRepository = requestRepository;
        this.workflowRepository = workflowRepository;
        this.userRepository = userRepository;
        this.requestService = requestService;
        this.workflowService = workflowService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public SearchResponse search(String query, SearchType type, Pageable pageable) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        boolean canSearchUsers = SecurityUtils.hasRole("ADMIN", "SUPER_ADMIN");

        List<RequestResponse> requests = Collections.emptyList();
        List<WorkflowResponse> workflows = Collections.emptyList();
        List<UserResponse> users = Collections.emptyList();

        // 1. Search Requests
        if (type == null || type == SearchType.REQUEST) {
            Specification<Request> spec = RequestSpecification.search(query)
                    .and((root, q, cb) -> cb.equal(root.get("tenantId"), tenantId));

            Page<Request> requestPage = requestRepository.findAll(spec, pageable);
            // Assuming we reuse RequestService mapper manually or loop
            requests = requestPage.stream()
                    .map(requestService::mapToResponse)
                    .toList();
        }

        // 2. Search Workflows
        if (type == null || type == SearchType.WORKFLOW) {
            Specification<Workflow> spec = WorkflowSpecification.search(query)
                    .and(WorkflowSpecification.withTenantId(tenantId));

            Page<Workflow> workflowPage = workflowRepository.findAll(spec, pageable);
            // Assuming we reuse WorkflowService mapper manually or loop
            workflows = workflowPage.stream()
                    .map(workflowService::mapToResponse)
                    .toList();
        }

        // 3. Search Users (Restricted)
        if (canSearchUsers && (type == null || type == SearchType.USER)) {
            Specification<User> spec = UserSpecification.search(query)
                    .and(UserSpecification.withTenantId(tenantId));

            Page<User> userPage = userRepository.findAll(spec, pageable);
            users = userPage.stream()
                    .map(userService::mapToResponse)
                    .toList();
        }

        return new SearchResponse(requests, workflows, users);
    }
}
