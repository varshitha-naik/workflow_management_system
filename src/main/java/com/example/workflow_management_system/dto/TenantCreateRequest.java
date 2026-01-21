package com.example.workflow_management_system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantCreateRequest(
                @NotBlank(message = "Tenant name must not be blank") String name,

                @NotBlank(message = "Admin username must not be blank") @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") String adminUsername,

                @NotBlank(message = "Admin email must not be blank") @Email(message = "Invalid email format") String adminEmail) {
}
