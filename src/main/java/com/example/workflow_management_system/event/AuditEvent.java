package com.example.workflow_management_system.event;

import java.util.Map;

public class AuditEvent {
    private final String entityType;
    private final String entityId;
    private final String action;
    private final String performedBy;
    private final Long tenantId;
    private final Map<String, Object> details;

    public AuditEvent(String entityType, String entityId, String action, String performedBy, Long tenantId,
            Map<String, Object> details) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.performedBy = performedBy;
        this.tenantId = tenantId;
        this.details = details;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getAction() {
        return action;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
