package com.example.workflow_management_system.controller;

import com.example.workflow_management_system.dto.RequestAssignmentResponse;
import com.example.workflow_management_system.service.RequestAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Assignments", description = "Request Assignment endpoints")
public class RequestAssignmentController {

    private final RequestAssignmentService requestAssignmentService;
    private final com.example.workflow_management_system.service.ExportService exportService;

    public RequestAssignmentController(RequestAssignmentService requestAssignmentService,
            com.example.workflow_management_system.service.ExportService exportService) {
        this.requestAssignmentService = requestAssignmentService;
        this.exportService = exportService;
    }

    @GetMapping("/assignments/export")
    public void exportAssignments(
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
                "attachment; filename=\"assignments_" + java.time.LocalDateTime.now() + "." + extension + "\"");

        java.util.List<RequestAssignmentResponse> assignments = requestAssignmentService.getMyAssignmentsForExport();
        exportService.exportAssignments(assignments, exportFormat, response.getOutputStream());
    }

    @GetMapping("/assignments/my")
    public ResponseEntity<org.springframework.data.domain.Page<RequestAssignmentResponse>> getMyAssignments(
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(requestAssignmentService.getMyAssignments(pageable));
    }

    @GetMapping("/requests/{id}/assignments")
    public ResponseEntity<List<RequestAssignmentResponse>> getAssignmentsByRequest(@PathVariable Long id) {
        return ResponseEntity.ok(requestAssignmentService.getAssignmentsByRequest(id));
    }
}
