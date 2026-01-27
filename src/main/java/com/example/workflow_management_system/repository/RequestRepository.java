package com.example.workflow_management_system.repository;

import com.example.workflow_management_system.model.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByTenantId(Long tenantId);

    Optional<Request> findByIdAndTenantId(Long id, Long tenantId);

    List<Request> findByTenantIdAndCreatedBy_Id(Long tenantId, Long userId);

    List<Request> findByTenantIdAndStatusAndCurrentStep_RequiredRole(Long tenantId,
            com.example.workflow_management_system.model.RequestStatus status,
            com.example.workflow_management_system.model.UserRole role);

    org.springframework.data.domain.Page<Request> findByTenantId(Long tenantId,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Request> findByTenantIdAndCreatedBy_Id(Long tenantId, Long userId,
            org.springframework.data.domain.Pageable pageable);
}
