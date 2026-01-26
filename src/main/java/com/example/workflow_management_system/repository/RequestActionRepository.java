package com.example.workflow_management_system.repository;

import com.example.workflow_management_system.model.RequestAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestActionRepository extends JpaRepository<RequestAction, Long> {
    List<RequestAction> findByRequestIdOrderByActionTimeAsc(Long requestId);

    List<RequestAction> findByRequestIdAndTenantIdOrderByActionTimeAsc(Long requestId, Long tenantId);
}
