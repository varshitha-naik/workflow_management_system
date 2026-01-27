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
                @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS") LocalDateTime createdAt,
                @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS") LocalDateTime updatedAt) {
}
