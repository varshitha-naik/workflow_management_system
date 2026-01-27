package com.example.workflow_management_system.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
                @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS") LocalDateTime timestamp,
                int status,
                String error,
                String message,
                String path) {
}
