package com.example.workflow_management_system.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class SecurityUtils {

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
