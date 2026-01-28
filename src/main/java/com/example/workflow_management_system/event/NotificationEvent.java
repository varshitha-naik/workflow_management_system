package com.example.workflow_management_system.event;

import java.util.Map;

public class NotificationEvent {
    private final NotificationType type;
    private final String recipientEmail;
    private final String tenantName;
    private final Map<String, Object> metadata;

    public NotificationEvent(NotificationType type, String recipientEmail, String tenantName,
            Map<String, Object> metadata) {
        this.type = type;
        this.recipientEmail = recipientEmail;
        this.tenantName = tenantName;
        this.metadata = metadata;
    }

    public NotificationType getType() {
        return type;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getTenantName() {
        return tenantName;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
