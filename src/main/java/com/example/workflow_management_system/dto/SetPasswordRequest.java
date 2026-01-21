package com.example.workflow_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetPasswordRequest(
        @NotBlank(message = "Token is required") String token,

        @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be at least 6 characters") String newPassword) {
}
