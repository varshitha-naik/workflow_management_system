package com.example.workflow_management_system.repository;

import com.example.workflow_management_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
        boolean existsByUsername(String username);

        java.util.Optional<User> findByUsername(String username);

        boolean existsByEmail(String email);

        java.util.Optional<User> findByEmail(String email);

        List<User> findByTenant_Id(Long tenantId);

        @org.springframework.data.jpa.repository.Query("SELECT u FROM User u LEFT JOIN FETCH u.tenant WHERE u.username = :username OR u.email = :email")
        java.util.Optional<User> findByUsernameOrEmail(
                        @org.springframework.data.repository.query.Param("username") String username,
                        @org.springframework.data.repository.query.Param("email") String email);

        // Updated queries to exclude deleted users
        boolean existsByTenant_IdAndRole(Long tenantId, com.example.workflow_management_system.model.UserRole role);

        org.springframework.data.domain.Page<User> findByTenant_IdAndDeletedFalse(Long tenantId,
                        org.springframework.data.domain.Pageable pageable);

        org.springframework.data.domain.Page<User> findByTenant_IdAndRoleAndDeletedFalse(Long tenantId,
                        com.example.workflow_management_system.model.UserRole role,
                        org.springframework.data.domain.Pageable pageable);

        org.springframework.data.domain.Page<User> findByTenant_IdAndActiveAndDeletedFalse(Long tenantId,
                        boolean active,
                        org.springframework.data.domain.Pageable pageable);

        org.springframework.data.domain.Page<User> findByTenant_IdAndRoleAndActiveAndDeletedFalse(Long tenantId,
                        com.example.workflow_management_system.model.UserRole role,
                        boolean active,
                        org.springframework.data.domain.Pageable pageable);

        boolean existsByRoleAndTenantIsNull(com.example.workflow_management_system.model.UserRole role);

        List<User> findByRoleAndTenantIsNull(com.example.workflow_management_system.model.UserRole role);
}
