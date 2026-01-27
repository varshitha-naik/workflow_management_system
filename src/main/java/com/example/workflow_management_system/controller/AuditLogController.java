package com.example.workflow_management_system.controller;

import com.example.workflow_management_system.model.AuditLog;
import com.example.workflow_management_system.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<org.springframework.data.domain.Page<AuditLog>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getAuditLogs(entityType, entityId, pageable));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<AuditLog>> getRecentAuditLogs() {
        return ResponseEntity.ok(auditLogService.getRecentAuditLogs());
    }
}
