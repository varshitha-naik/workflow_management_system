package com.example.workflow_management_system.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(
        @NotNull(message = "Active status must be provided") Boolean active) {
}
