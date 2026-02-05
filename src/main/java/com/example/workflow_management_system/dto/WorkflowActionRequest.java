package com.example.workflow_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkflowActionRequest(
        @NotBlank(message = "Name is required") String name,
        @NotNull(message = "Workflow ID is required") Long workflowId) {
}
