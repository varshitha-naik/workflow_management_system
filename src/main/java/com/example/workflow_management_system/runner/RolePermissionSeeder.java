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
        // ADMIN Permissions
        seedPermission("ADMIN", "workflow:read");
        seedPermission("ADMIN", "workflow:write");
        seedPermission("ADMIN", "request:read");
        seedPermission("ADMIN", "request:write");
        seedPermission("ADMIN", "user:read");
        seedPermission("ADMIN", "user:write");
        seedPermission("ADMIN", "tenant:read"); // View tenant details

        // USER Permissions (Read-only interpretation)
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
