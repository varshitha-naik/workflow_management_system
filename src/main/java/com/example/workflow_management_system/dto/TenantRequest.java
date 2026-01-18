package com.example.workflow_management_system.dto;

import jakarta.validation.constraints.NotBlank;

public record TenantRequest(@NotBlank(message = "Tenant name must not be blank") String name) {
}
