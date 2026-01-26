package com.example.workflow_management_system.dto;

import com.example.workflow_management_system.model.AssignmentStatus;
import java.time.LocalDateTime;

public record RequestAssignmentResponse(
        Long id,
        Long requestId,
        Long assignedToUserId,
        String assignedToUsername,
        LocalDateTime assignedAt,
        LocalDateTime dueAt,
        AssignmentStatus status) {
}
