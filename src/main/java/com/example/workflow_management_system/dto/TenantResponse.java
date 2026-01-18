package com.example.workflow_management_system.dto;

import com.example.workflow_management_system.model.TenantStatus;
import java.time.LocalDateTime;

public record TenantResponse(
        Long id,
        String name,
        TenantStatus status,
        LocalDateTime createdAt) {
}
