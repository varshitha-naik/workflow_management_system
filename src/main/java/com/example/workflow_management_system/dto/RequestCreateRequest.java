package com.example.workflow_management_system.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record RequestCreateRequest(
        @NotNull(message = "Workflow ID is required") Long workflowId,
        Map<String, Object> payload) {
}
