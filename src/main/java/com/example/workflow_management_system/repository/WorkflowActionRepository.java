package com.example.workflow_management_system.repository;

import com.example.workflow_management_system.model.WorkflowAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowActionRepository extends JpaRepository<WorkflowAction, Long> {
    List<WorkflowAction> findByTenantId(Long tenantId);

    List<WorkflowAction> findByTenantIdAndActiveTrue(Long tenantId);
}
