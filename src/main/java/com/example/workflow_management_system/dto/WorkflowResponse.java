package com.example.workflow_management_system.dto;

import java.time.LocalDateTime;

public record WorkflowResponse(
        Long id,
        String name,
        String description,
        Long tenantId,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
