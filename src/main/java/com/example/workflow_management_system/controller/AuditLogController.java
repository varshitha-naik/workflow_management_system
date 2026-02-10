package com.example.workflow_management_system.controller;

import com.example.workflow_management_system.model.AuditLog;
import com.example.workflow_management_system.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit Logs", description = "Audit Log endpoints")
public class AuditLogController {

        private final AuditLogService auditLogService;
        private final com.example.workflow_management_system.service.ExportService exportService;

        public AuditLogController(AuditLogService auditLogService,
                        com.example.workflow_management_system.service.ExportService exportService) {
                this.auditLogService = auditLogService;
                this.exportService = exportService;
        }

        @GetMapping("/export")
        @PreAuthorize("hasAnyRole('GLOBAL_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER')")
        public void exportAuditLogs(
                        @RequestParam(required = false) String entityType,
                        @RequestParam(required = false) String entityId,
                        @RequestParam(required = false) String action,
                        @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime fromDate,
                        @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime toDate,
                        @RequestParam(defaultValue = "csv") String format,
                        jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {

                com.example.workflow_management_system.service.ExportService.ExportFormat exportFormat;
                try {
                        exportFormat = com.example.workflow_management_system.service.ExportService.ExportFormat
                                        .valueOf(format.toUpperCase());
                } catch (IllegalArgumentException e) {
                        response.sendError(org.springframework.http.HttpStatus.BAD_REQUEST.value(),
                                        "Invalid format. Supported: csv, xlsx");
                        return;
                }

                response.setContentType(
                                exportFormat == com.example.workflow_management_system.service.ExportService.ExportFormat.CSV
                                                ? "text/csv"
                                                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                String extension = exportFormat == com.example.workflow_management_system.service.ExportService.ExportFormat.CSV
                                ? "csv"
                                : "xlsx";
                response.setHeader("Content-Disposition",
                                "attachment; filename=\"audit_logs_" + java.time.LocalDateTime.now() + "." + extension
                                                + "\"");

                java.util.List<AuditLog> logs = auditLogService.getAuditLogsForExport(entityType, entityId, action,
                                fromDate,
                                toDate);
                exportService.exportAuditLogs(logs, exportFormat, response.getOutputStream());
        }

        @GetMapping
        @PreAuthorize("hasAnyRole('GLOBAL_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER')")
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

                pageable = com.example.workflow_management_system.util.PaginationUtils.validateAndApplyDefaults(
                                pageable,
                                allowedFields, defaultSort);

                return ResponseEntity
                                .ok(auditLogService.getAuditLogs(entityType, entityId, action, fromDate, toDate,
                                                pageable));
        }

        @GetMapping("/recent")
        @PreAuthorize("hasAnyRole('GLOBAL_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER')")
        public ResponseEntity<List<AuditLog>> getRecentAuditLogs() {
                return ResponseEntity.ok(auditLogService.getRecentAuditLogs());
        }
}
