package com.example.workflow_management_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableAsync
public class WorkflowManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowManagementSystemApplication.class, args);
    }
}
