package com.example.workflow_management_system.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_steps", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "workflow_id", "step_order" })
}, indexes = {
        @Index(name = "idx_workflow_step_workflow", columnList = "workflow_id")
})
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "workflow_id IN (select w.id from workflows w where w.tenant_id = :tenantId)")
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_role", nullable = false)
    private UserRole requiredRole;

    @Column(name = "auto_approve", nullable = false)
    private boolean autoApprove;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public WorkflowStep() {
    }

    public WorkflowStep(Workflow workflow, int stepOrder, String stepName, UserRole requiredRole, boolean autoApprove) {
        this.workflow = workflow;
        this.stepOrder = stepOrder;
        this.stepName = stepName;
        this.requiredRole = requiredRole;
        this.autoApprove = autoApprove;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public UserRole getRequiredRole() {
        return requiredRole;
    }

    public void setRequiredRole(UserRole requiredRole) {
        this.requiredRole = requiredRole;
    }

    public boolean isAutoApprove() {
        return autoApprove;
    }

    public void setAutoApprove(boolean autoApprove) {
        this.autoApprove = autoApprove;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
