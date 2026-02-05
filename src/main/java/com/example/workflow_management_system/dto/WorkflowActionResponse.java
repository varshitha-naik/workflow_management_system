package com.example.workflow_management_system.dto;

import java.time.LocalDateTime;

public record WorkflowActionResponse(
        Long id,
        String name,
        Long workflowId,
        String workflowName,
        boolean active,
        LocalDateTime createdAt) {
}
