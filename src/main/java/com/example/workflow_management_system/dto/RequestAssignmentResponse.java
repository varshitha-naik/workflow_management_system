package com.example.workflow_management_system.dto;

import com.example.workflow_management_system.model.AssignmentStatus;
import java.time.LocalDateTime;

public record RequestAssignmentResponse(
                Long id,
                Long requestId,
                Long assignedToUserId,
                String assignedToUsername,
                @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS") LocalDateTime assignedAt,
                @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS") LocalDateTime dueAt,
                AssignmentStatus status) {
}
