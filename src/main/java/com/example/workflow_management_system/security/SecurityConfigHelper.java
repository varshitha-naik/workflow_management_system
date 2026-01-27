package com.example.workflow_management_system.security;

import com.example.workflow_management_system.service.RolePermissionService;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class SecurityConfigHelper {

    private final RolePermissionService rolePermissionService;

    public SecurityConfigHelper(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @PostConstruct
    public void init() {
        SecurityUtils.setRolePermissionService(rolePermissionService);
    }
}
