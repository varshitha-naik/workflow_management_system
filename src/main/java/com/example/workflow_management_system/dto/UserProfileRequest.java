package com.example.workflow_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileRequest(
        @NotBlank(message = "Username cannot be blank") String username,

        @Size(min = 6, message = "Password must be at least 6 characters") String password) {
}
