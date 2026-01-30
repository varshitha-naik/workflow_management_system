package com.example.workflow_management_system.service;

import com.example.workflow_management_system.dto.RequestAssignmentResponse;
import com.example.workflow_management_system.dto.RequestResponse;
import com.example.workflow_management_system.model.AuditLog;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

@Service
public class ExportService {

    public enum ExportFormat {
        CSV, XLSX
    }

    public void exportRequests(List<RequestResponse> requests, ExportFormat format, OutputStream outputStream)
            throws IOException {
        if (format == ExportFormat.CSV) {
            exportRequestsCsv(requests, outputStream);
        } else {
            exportRequestsExcel(requests, outputStream);
        }
    }

    public void exportAssignments(List<RequestAssignmentResponse> assignments, ExportFormat format,
            OutputStream outputStream) throws IOException {
        if (format == ExportFormat.CSV) {
            exportAssignmentsCsv(assignments, outputStream);
        } else {
            exportAssignmentsExcel(assignments, outputStream);
        }
    }

    public void exportAuditLogs(List<AuditLog> auditLogs, ExportFormat format, OutputStream outputStream)
            throws IOException {
        if (format == ExportFormat.CSV) {
            exportAuditLogsCsv(auditLogs, outputStream);
        } else {
            exportAuditLogsExcel(auditLogs, outputStream);
        }
    }

    private void exportRequestsCsv(List<RequestResponse> requests, OutputStream outputStream) throws IOException {
        try (Writer writer = new OutputStreamWriter(outputStream);
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                        .setHeader("ID", "Workflow", "Status", "Current Step", "Created By", "Created At").build())) {
            for (RequestResponse request : requests) {
                csvPrinter.printRecord(
                        request.id(),
                        request.workflowName(),
                        request.status(),
                        request.currentStepName(),
                        request.createdByUsername(),
                        request.createdAt());
            }
        }
    }

    private void exportRequestsExcel(List<RequestResponse> requests, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Requests");
            Row headerRow = sheet.createRow(0);
            String[] headers = { "ID", "Workflow", "Status", "Current Step", "Created By", "Created At" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNum = 1;
            for (RequestResponse request : requests) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(request.id());
                row.createCell(1).setCellValue(request.workflowName());
                row.createCell(2).setCellValue(request.status().toString());
                row.createCell(3).setCellValue(request.currentStepName());
                row.createCell(4).setCellValue(request.createdByUsername());
                row.createCell(5).setCellValue(request.createdAt().toString());
            }
            workbook.write(outputStream);
        }
    }

    private void exportAssignmentsCsv(List<RequestAssignmentResponse> assignments, OutputStream outputStream)
            throws IOException {
        try (Writer writer = new OutputStreamWriter(outputStream);
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                        .setHeader("ID", "Request ID", "Assigned To", "Status", "Assigned At", "Due At").build())) {
            for (RequestAssignmentResponse assignment : assignments) {
                csvPrinter.printRecord(
                        assignment.id(),
                        assignment.requestId(),
                        assignment.assignedToUsername(),
                        assignment.status(),
                        assignment.assignedAt(),
                        assignment.dueAt());
            }
        }
    }

    private void exportAssignmentsExcel(List<RequestAssignmentResponse> assignments, OutputStream outputStream)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Assignments");
            Row headerRow = sheet.createRow(0);
            String[] headers = { "ID", "Request ID", "Assigned To", "Status", "Assigned At", "Due At" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNum = 1;
            for (RequestAssignmentResponse assignment : assignments) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(assignment.id());
                row.createCell(1).setCellValue(assignment.requestId());
                row.createCell(2).setCellValue(assignment.assignedToUsername());
                row.createCell(3).setCellValue(assignment.status().toString());
                row.createCell(4).setCellValue(assignment.assignedAt().toString());
                if (assignment.dueAt() != null) {
                    row.createCell(5).setCellValue(assignment.dueAt().toString());
                }
            }
            workbook.write(outputStream);
        }
    }

    private void exportAuditLogsCsv(List<AuditLog> auditLogs, OutputStream outputStream) throws IOException {
        try (Writer writer = new OutputStreamWriter(outputStream);
                CSVPrinter csvPrinter = new CSVPrinter(writer,
                        CSVFormat.DEFAULT.builder()
                                .setHeader("ID", "Action", "Entity Type", "Entity ID", "Actor", "Timestamp", "Details")
                                .build())) {
            for (AuditLog log : auditLogs) {
                csvPrinter.printRecord(
                        log.getId(),
                        log.getAction(),
                        log.getEntityType(),
                        log.getEntityId(),
                        log.getPerformedBy(),
                        log.getTimestamp(),
                        log.getDetails());
            }
        }
    }

    private void exportAuditLogsExcel(List<AuditLog> auditLogs, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Audit Logs");
            Row headerRow = sheet.createRow(0);
            String[] headers = { "ID", "Action", "Entity Type", "Entity ID", "Actor", "Timestamp", "Details" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNum = 1;
            for (AuditLog log : auditLogs) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(log.getId());
                row.createCell(1).setCellValue(log.getAction());
                row.createCell(2).setCellValue(log.getEntityType());
                row.createCell(3).setCellValue(log.getEntityId());
                row.createCell(4).setCellValue(log.getPerformedBy());
                row.createCell(5).setCellValue(log.getTimestamp().toString());
                row.createCell(6).setCellValue(log.getDetails());
            }
            workbook.write(outputStream);
        }
    }
}
