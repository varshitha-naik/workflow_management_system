package com.example.workflow_management_system.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkflowStepActionRequest(
        @NotBlank(message = "Action name is required") String name,
        @NotBlank(message = "Result Status is required") String resultStatus) {
}
