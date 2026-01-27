package com.example.workflow_management_system.dto;

import com.example.workflow_management_system.model.TenantStatus;
import java.time.LocalDateTime;

public record TenantResponse(
                Long id,
                String name,
                TenantStatus status,
                @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS") LocalDateTime createdAt) {
}
