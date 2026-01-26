package com.example.workflow_management_system.dto;

import com.example.workflow_management_system.model.ActionType;
import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.NotNull;

public record RequestActionCreateRequest(
        @NotNull(message = "Action type is required") ActionType actionType,
        @Nullable String comments) {
}
