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

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "request", "request.workflow",
                        "request.currentStep" })
        List<RequestAssignment> findByAssignedTo_IdAndTenantIdOrderByAssignedAtDesc(Long userId, Long tenantId);

        List<RequestAssignment> findByRequestIdAndTenantIdOrderByAssignedAtAsc(Long requestId, Long tenantId);

        List<RequestAssignment> findAllByStatusAndDueAtBefore(AssignmentStatus status, java.time.LocalDateTime dueAt);

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "request", "request.workflow",
                        "request.currentStep" })
        org.springframework.data.domain.Page<RequestAssignment> findByAssignedTo_IdAndTenantIdOrderByAssignedAtDesc(
                        Long userId, Long tenantId, org.springframework.data.domain.Pageable pageable);
}
