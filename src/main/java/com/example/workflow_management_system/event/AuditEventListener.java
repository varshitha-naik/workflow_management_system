package com.example.workflow_management_system.event;

import com.example.workflow_management_system.model.AuditLog;
import com.example.workflow_management_system.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditEventListener(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    @EventListener
    public void handleAuditEvent(AuditEvent event) {
        String detailsJson = "{}";
        if (event.getDetails() != null) {
            try {
                detailsJson = objectMapper.writeValueAsString(event.getDetails());
            } catch (JsonProcessingException e) {
                // Ignore error, keep empty or log error if needed
                // Since requirement is "Failures in audit logging must NOT affect main API
                // flow",
                // ignoring here is safe.
            }
        }

        AuditLog auditLog = new AuditLog(
                event.getEntityType(),
                event.getEntityId(),
                event.getAction(),
                event.getPerformedBy(),
                event.getTenantId(),
                detailsJson);

        auditLogRepository.save(auditLog);
    }
}
