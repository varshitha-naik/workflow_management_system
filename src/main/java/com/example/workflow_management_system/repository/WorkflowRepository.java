package com.example.workflow_management_system.repository;

import com.example.workflow_management_system.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long>, JpaSpecificationExecutor<Workflow> {
    List<Workflow> findByTenantId(Long tenantId);

    boolean existsByNameAndTenantId(String name, Long tenantId);

    org.springframework.data.domain.Page<Workflow> findByTenantId(Long tenantId,
            org.springframework.data.domain.Pageable pageable);
}
