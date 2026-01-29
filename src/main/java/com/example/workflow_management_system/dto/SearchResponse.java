package com.example.workflow_management_system.dto;

import java.util.List;

public record SearchResponse(
        List<RequestResponse> requests,
        List<WorkflowResponse> workflows,
        List<UserResponse> users) {
}
