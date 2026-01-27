package com.example.workflow_management_system.service;

import com.example.workflow_management_system.model.RolePermission;
import com.example.workflow_management_system.repository.RolePermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;

    public RolePermissionService(RolePermissionRepository rolePermissionRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(String roleName, String permissionKey) {
        if ("SUPER_ADMIN".equals(roleName)) {
            return true;
        }
        return rolePermissionRepository.existsByRoleNameAndPermissionKey(roleName, permissionKey);
    }

    public void addPermission(String roleName, String permissionKey, Long tenantId) {
        // Avoid duplicates (ignoring tenant for simplicity check logic or strictly
        // unique?)
        // For now, allow duplicates if tenant differs or just explicit add
        if (!rolePermissionRepository.existsByRoleNameAndPermissionKey(roleName, permissionKey)) {
            RolePermission rp = new RolePermission(roleName, permissionKey, tenantId);
            rolePermissionRepository.save(rp);
        }
    }
}
