package com.example.workflow_management_system.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_keys", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "key_value", "tenant_id" })
})
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_value", nullable = false)
    private String key;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "request_path", nullable = false)
    private String requestPath;

    @Column(name = "request_checksum", nullable = false)
    private String requestChecksum;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public IdempotencyKey() {
    }

    public IdempotencyKey(String key, Long tenantId, String requestPath, String requestChecksum, int responseStatus,
            String responseBody) {
        this.key = key;
        this.tenantId = tenantId;
        this.requestPath = requestPath;
        this.requestChecksum = requestChecksum;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getRequestChecksum() {
        return requestChecksum;
    }

    public void setRequestChecksum(String requestChecksum) {
        this.requestChecksum = requestChecksum;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
