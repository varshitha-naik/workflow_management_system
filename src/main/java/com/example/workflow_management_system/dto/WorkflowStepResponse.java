package com.example.workflow_management_system.dto;

import com.example.workflow_management_system.model.UserRole;
import java.time.LocalDateTime;

public record WorkflowStepResponse(
        Long id,
        Long workflowId,
        int stepOrder,
        String stepName,
        UserRole requiredRole,
        boolean autoApprove,
        LocalDateTime createdAt) {
}
