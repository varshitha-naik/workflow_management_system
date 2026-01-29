package com.example.workflow_management_system.controller;

import com.example.workflow_management_system.model.AuditLog;
import com.example.workflow_management_system.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
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
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime toDate,
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {

        java.util.Set<String> allowedFields = java.util.Set.of("id", "timestamp", "entityType", "action");
        org.springframework.data.domain.Sort defaultSort = org.springframework.data.domain.Sort
                .by(org.springframework.data.domain.Sort.Direction.DESC, "timestamp");

        pageable = com.example.workflow_management_system.util.PaginationUtils.validateAndApplyDefaults(pageable,
                allowedFields, defaultSort);

        return ResponseEntity
                .ok(auditLogService.getAuditLogs(entityType, entityId, action, fromDate, toDate, pageable));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<AuditLog>> getRecentAuditLogs() {
        return ResponseEntity.ok(auditLogService.getRecentAuditLogs());
    }
}
