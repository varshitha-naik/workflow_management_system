package com.example.workflow_management_system.dto;

import com.example.workflow_management_system.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserRequest(
                @NotBlank(message = "Username is required") String username,

                @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,

                @NotNull(message = "Role is required") UserRole role) {
}
