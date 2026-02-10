package com.example.workflow_management_system.controller;

import com.example.workflow_management_system.dto.TenantRequest;
import com.example.workflow_management_system.dto.TenantResponse;
import com.example.workflow_management_system.dto.UserResponse;
import com.example.workflow_management_system.service.TenantService;
import com.example.workflow_management_system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/tenants")
@Validated
public class TenantController {

    private final TenantService tenantService;
    private final UserService userService;

    public TenantController(TenantService tenantService, UserService userService) {
        this.tenantService = tenantService;
        this.userService = userService;
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<TenantResponse> createTenant(
            @Valid @RequestBody com.example.workflow_management_system.dto.TenantCreateRequest request) {

        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TenantController.class);
        com.example.workflow_management_system.security.UserPrincipal currentUser = com.example.workflow_management_system.security.SecurityUtils
                .getCurrentUser();

        logger.info("LIVE_TRACE: TenantController - Received createTenant request");
        logger.info("LIVE_TRACE: TenantController - Principal: {}", currentUser.getUsername());
        logger.info("LIVE_TRACE: TenantController - Authorities: {}", currentUser.getAuthorities());
        logger.info("LIVE_TRACE: TenantController - TenantContext ID: {}",
                com.example.workflow_management_system.security.TenantContext.getTenantId());

        if (currentUser.getTenantId() != null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only Global Admin can create tenants.");
        }

        TenantResponse response = tenantService.createTenant(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<List<TenantResponse>> getAllTenants() {
        return ResponseEntity.ok(tenantService.getAllTenants());
    }

    @GetMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('GLOBAL_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER', 'USER')")
    public ResponseEntity<TenantResponse> getTenantById(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(tenantService.getTenantById(id));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('GLOBAL_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<TenantResponse> updateTenant(@PathVariable @Min(1) Long id,
            @Valid @RequestBody TenantRequest request) {
        return ResponseEntity.ok(tenantService.updateTenant(id, request));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<Void> deleteTenant(@PathVariable @Min(1) Long id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/users")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('GLOBAL_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER', 'USER')")
    public ResponseEntity<List<UserResponse>> getUsersByTenant(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUsersByTenant(id));
    }
}
