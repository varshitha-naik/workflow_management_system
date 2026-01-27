package com.example.workflow_management_system.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.example.workflow_management_system.service.RolePermissionService;

public class SecurityUtils {

    private static RolePermissionService rolePermissionService;

    public static void setRolePermissionService(RolePermissionService service) {
        rolePermissionService = service;
    }

    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated user");
        }
        return (UserPrincipal) authentication.getPrincipal();
    }

    public static Long getCurrentTenantId() {
        return getCurrentUser().getTenantId();
    }

    public static boolean hasPermission(String permissionKey) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return false;
        }
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        // Fallback or explicit check
        if (rolePermissionService == null) {
            // Should properly log or handle not initialized.
            // For now, fail safe by returning false as per verification request.
            return false;
        }
        return rolePermissionService.hasPermission(user.getRole(), permissionKey);
    }

    public static void validateTenantAccess(Long tenantId) {
        UserPrincipal currentUser = getCurrentUser();
        // Super Admins can access any tenant
        if ("SUPER_ADMIN".equals(currentUser.getRole())) {
            return;
        }

        Long currentTenantId = currentUser.getTenantId();
        if (currentTenantId != null && !currentTenantId.equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Cross-tenant access is not allowed");
        }
    }
}
