package com.example.workflow_management_system.service;

import com.example.workflow_management_system.model.AuditLog;
import com.example.workflow_management_system.repository.AuditLogRepository;
import com.example.workflow_management_system.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void logEvent(String entityType, String entityId, String action, Map<String, Object> details) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        String performedBy = "SYSTEM";
        try {
            Long userId = SecurityUtils.getCurrentUser().getId();
            if (userId != null) {
                performedBy = String.valueOf(userId);
            }
        } catch (Exception e) {
            // Likely system action or no auth context
        }

        String detailsJson = "{}";
        if (details != null) {
            try {
                detailsJson = objectMapper.writeValueAsString(details);
            } catch (JsonProcessingException e) {
                // Ignore error, keep empty
            }
        }

        AuditLog log = new AuditLog(entityType, entityId, action, performedBy, tenantId, detailsJson);
        auditLogRepository.save(log);
    }

    // Allow logging with explicit user ID (e.g. when context is not available or
    // for system jobs masquerading users)
    public void logEvent(String entityType, String entityId, String action, String performedBy, Long tenantId,
            Map<String, Object> details) {
        String detailsJson = "{}";
        if (details != null) {
            try {
                detailsJson = objectMapper.writeValueAsString(details);
            } catch (JsonProcessingException e) {
                // Ignore
            }
        }
        AuditLog log = new AuditLog(entityType, entityId, action, performedBy, tenantId, detailsJson);
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AuditLog> getAuditLogs(String entityType, String entityId,
            org.springframework.data.domain.Pageable pageable) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        if (entityType != null && entityId != null) {
            return auditLogRepository.findByEntityTypeAndEntityIdAndTenantIdOrderByTimestampDesc(entityType, entityId,
                    tenantId, pageable); // Mismatch in args? Repository method needs pageable if I promised to use it.
        }
        return auditLogRepository.findByTenantIdOrderByTimestampDesc(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getRecentAuditLogs() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return auditLogRepository.findTop20ByTenantIdOrderByTimestampDesc(tenantId);
    }
}
