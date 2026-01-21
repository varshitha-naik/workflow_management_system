package com.example.workflow_management_system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email) {
}
