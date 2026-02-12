package com.example.workflow_management_system.dto;

import java.time.LocalDateTime;

public record WorkflowResponse(
        Long id,
        String name,
        String description,
        Long tenantId,
        boolean active,
        String firstStepRole,
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS") LocalDateTime createdAt,
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS") LocalDateTime updatedAt) {
}
