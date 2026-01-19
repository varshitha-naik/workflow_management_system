package com.example.workflow_management_system.dto;

import com.example.workflow_management_system.model.UserRole;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        UserRole role,
        boolean active,
        Long tenantId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
