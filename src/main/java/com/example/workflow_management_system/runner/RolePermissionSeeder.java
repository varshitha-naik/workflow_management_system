package com.example.workflow_management_system.runner;

import com.example.workflow_management_system.service.RolePermissionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RolePermissionSeeder implements CommandLineRunner {

    private final RolePermissionService rolePermissionService;

    public RolePermissionSeeder(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @Override
    public void run(String... args) throws Exception {
        // TENANT_MANAGER Permissions (Formerly ADMIN)
        seedPermission("TENANT_MANAGER", "workflow:read");
        seedPermission("TENANT_MANAGER", "workflow:write");
        seedPermission("TENANT_MANAGER", "request:read");
        seedPermission("TENANT_MANAGER", "request:write");
        seedPermission("TENANT_MANAGER", "user:read");
        seedPermission("TENANT_MANAGER", "user:write");
        seedPermission("TENANT_MANAGER", "tenant:read"); // View tenant details

        // TENANT_ADMIN Permissions (Formerly SUPER_ADMIN)
        // Ensure TENANT_ADMIN has full permissions
        seedPermission("TENANT_ADMIN", "workflow:read");
        seedPermission("TENANT_ADMIN", "workflow:write");
        seedPermission("TENANT_ADMIN", "request:read");
        seedPermission("TENANT_ADMIN", "request:write");
        seedPermission("TENANT_ADMIN", "user:read");
        seedPermission("TENANT_ADMIN", "user:write");
        seedPermission("TENANT_ADMIN", "tenant:read");

        // USER Permissions
        seedPermission("USER", "workflow:read");
        seedPermission("USER", "request:read");
        // Typically users also create requests, but strict 'read-only' requested:
        // seedPermission("USER", "request:create");
    }

    private void seedPermission(String role, String permissionKey) {
        // tenantId null for global defaults
        rolePermissionService.addPermission(role, permissionKey, null);
    }
}
