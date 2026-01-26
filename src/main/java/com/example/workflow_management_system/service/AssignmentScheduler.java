package com.example.workflow_management_system.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AssignmentScheduler {

    private final RequestAssignmentService requestAssignmentService;

    public AssignmentScheduler(RequestAssignmentService requestAssignmentService) {
        this.requestAssignmentService = requestAssignmentService;
    }

    @Scheduled(fixedRate = 300000) // 5 minutes = 300,000 ms
    public void checkOverdueAssignments() {
        requestAssignmentService.markOverdueAssignments();
    }
}
