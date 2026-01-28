package com.example.workflow_management_system.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_actions", indexes = {
        @Index(name = "idx_request_action_request", columnList = "request_id"),
        @Index(name = "idx_request_action_tenant", columnList = "tenant_id"),
        @Index(name = "idx_request_action_time", columnList = "action_time")
})
public class RequestAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_by")
    private User actionBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_step_id", nullable = false)
    private WorkflowStep fromStep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_step_id")
    private WorkflowStep toStep;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @CreationTimestamp
    @Column(name = "action_time", nullable = false, updatable = false)
    private LocalDateTime actionTime;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public RequestAction() {
    }

    public RequestAction(Request request, User actionBy, ActionType actionType, WorkflowStep fromStep,
            WorkflowStep toStep, String comments, Long tenantId) {
        this.request = request;
        this.actionBy = actionBy;
        this.actionType = actionType;
        this.fromStep = fromStep;
        this.toStep = toStep;
        this.comments = comments;
        this.tenantId = tenantId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public User getActionBy() {
        return actionBy;
    }

    public void setActionBy(User actionBy) {
        this.actionBy = actionBy;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public WorkflowStep getFromStep() {
        return fromStep;
    }

    public void setFromStep(WorkflowStep fromStep) {
        this.fromStep = fromStep;
    }

    public WorkflowStep getToStep() {
        return toStep;
    }

    public void setToStep(WorkflowStep toStep) {
        this.toStep = toStep;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public LocalDateTime getActionTime() {
        return actionTime;
    }

    public void setActionTime(LocalDateTime actionTime) {
        this.actionTime = actionTime;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
