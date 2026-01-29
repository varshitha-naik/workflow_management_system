package com.example.workflow_management_system.repository;

import com.example.workflow_management_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
        boolean existsByUsername(String username);

        boolean existsByEmail(String email);

        List<User> findByTenant_Id(Long tenantId);

        @org.springframework.data.jpa.repository.Query("SELECT u FROM User u LEFT JOIN FETCH u.tenant WHERE u.username = :username OR u.email = :email")
        java.util.Optional<User> findByUsernameOrEmail(
                        @org.springframework.data.repository.query.Param("username") String username,
                        @org.springframework.data.repository.query.Param("email") String email);

        org.springframework.data.domain.Page<User> findByTenant_Id(Long tenantId,
                        org.springframework.data.domain.Pageable pageable);
}
