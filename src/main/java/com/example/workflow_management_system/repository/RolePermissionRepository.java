package com.example.workflow_management_system.repository;

import com.example.workflow_management_system.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    
    boolean existsByRoleNameAndPermissionKey(String roleName, String permissionKey);
    
    List<RolePermission> findByRoleName(String roleName);
}
