package com.example.workflow_management_system.dto;

import com.example.workflow_management_system.model.ActionType;
import java.time.LocalDateTime;

public record RequestActionResponse(
        Long id,
        Long requestId,
        Long actionByUserId,
        String actionByUsername,
        ActionType actionType,
        Long fromStepId,
        String fromStepName,
        Long toStepId,
        String toStepName,
        String comments,
        LocalDateTime actionTime) {
}
