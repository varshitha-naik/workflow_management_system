package com.example.workflow_management_system.dto;

import com.example.workflow_management_system.model.UserRole;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkflowStepRequest(
        @Min(value = 1, message = "Step order must be at least 1") int stepOrder,
        @NotBlank(message = "Step name is required") String stepName,
        @NotNull(message = "Required role is required") UserRole requiredRole,
        boolean autoApprove) {
}
