package com.example.workflow_management_system.dto;

import com.example.workflow_management_system.model.RequestStatus;
import java.time.LocalDateTime;
import java.util.Map;

public record RequestResponse(
        Long id,
        Long workflowId,
        String workflowName,
        Long createdByUserId,
        String createdByUsername,
        Long currentStepId,
        String currentStepName,
        RequestStatus status,
        Map<String, Object> payload,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
