package com.example.workflow_management_system.repository;

import com.example.workflow_management_system.model.AssignmentStatus;
import com.example.workflow_management_system.model.RequestAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestAssignmentRepository extends JpaRepository<RequestAssignment, Long> {

    Optional<RequestAssignment> findByRequestIdAndStatusAndTenantId(Long requestId, AssignmentStatus status,
            Long tenantId);

    List<RequestAssignment> findByAssignedTo_IdAndTenantIdOrderByAssignedAtDesc(Long userId, Long tenantId);

    List<RequestAssignment> findByRequestIdAndTenantIdOrderByAssignedAtAsc(Long requestId, Long tenantId);

    List<RequestAssignment> findAllByStatusAndDueAtBefore(AssignmentStatus status, java.time.LocalDateTime dueAt);
}
