package com.example.workflow_management_system.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username or email is required") String usernameOrEmail,
        @NotBlank(message = "Password is required") String password) {
}
