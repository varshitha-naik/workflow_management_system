package com.example.workflow_management_system.repository;

import com.example.workflow_management_system.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>,
                org.springframework.data.jpa.repository.JpaSpecificationExecutor<AuditLog> {
        List<AuditLog> findByEntityTypeAndEntityIdAndTenantIdOrderByTimestampDesc(String entityType, String entityId,
                        Long tenantId);

        List<AuditLog> findByTenantIdOrderByTimestampDesc(Long tenantId);

        List<AuditLog> findTop20ByTenantIdOrderByTimestampDesc(Long tenantId);

        org.springframework.data.domain.Page<AuditLog> findByTenantIdOrderByTimestampDesc(Long tenantId,
                        org.springframework.data.domain.Pageable pageable);

        org.springframework.data.domain.Page<AuditLog> findByEntityTypeAndEntityIdAndTenantIdOrderByTimestampDesc(
                        String entityType, String entityId, Long tenantId,
                        org.springframework.data.domain.Pageable pageable);
}
