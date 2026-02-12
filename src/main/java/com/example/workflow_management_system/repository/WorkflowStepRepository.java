package com.example.workflow_management_system.repository;

import com.example.workflow_management_system.model.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
        List<WorkflowStep> findByWorkflowIdOrderByStepOrderAsc(Long workflowId);

        boolean existsByWorkflowIdAndStepOrder(Long workflowId, int stepOrder);

        java.util.Optional<WorkflowStep> findFirstByWorkflowIdAndStepOrderGreaterThanOrderByStepOrderAsc(
                        Long workflowId,
                        int stepOrder);

        org.springframework.data.domain.Page<WorkflowStep> findByWorkflowIdOrderByStepOrderAsc(Long workflowId,
                        org.springframework.data.domain.Pageable pageable);

        java.util.Optional<WorkflowStep> findByWorkflowIdAndStepOrder(Long workflowId, int stepOrder);
}
