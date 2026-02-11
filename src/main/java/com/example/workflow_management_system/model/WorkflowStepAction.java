package com.example.workflow_management_system.model;

import jakarta.persistence.*;

@Entity
@Table(name = "workflow_step_actions")
public class WorkflowStepAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private WorkflowStep step;

    @Column(name = "action_name", nullable = false)
    private String name;

    @Column(name = "result_status", nullable = false)
    private String resultStatus;

    public WorkflowStepAction() {
    }

    public WorkflowStepAction(WorkflowStep step, String name, String resultStatus) {
        this.step = step;
        this.name = name;
        this.resultStatus = resultStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkflowStep getStep() {
        return step;
    }

    public void setStep(WorkflowStep step) {
        this.step = step;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }
}
