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
}
