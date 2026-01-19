package com.example.workflow_management_system.dto;

import com.example.workflow_management_system.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "Username is required") String username,

        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,

        @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be at least 6 characters") String password,

        @NotNull(message = "Role is required") UserRole role,

        @NotNull(message = "Tenant ID is required") Long tenantId,

        boolean active) {
}
