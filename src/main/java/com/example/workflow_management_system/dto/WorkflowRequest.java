package com.example.workflow_management_system.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkflowRequest(
        @NotBlank(message = "Name is required") String name,
        String description,
        boolean active) {
}
