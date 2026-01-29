package com.example.workflow_management_system.controller;

import com.example.workflow_management_system.dto.RequestActionCreateRequest;
import com.example.workflow_management_system.dto.RequestActionResponse;
import com.example.workflow_management_system.service.RequestActionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/requests/{requestId}/actions")
public class RequestActionController {

    private final RequestActionService requestActionService;

    public RequestActionController(RequestActionService requestActionService) {
        this.requestActionService = requestActionService;
    }

    @PostMapping
    @com.example.workflow_management_system.aspect.Idempotent
    public ResponseEntity<RequestActionResponse> createAction(
            @PathVariable Long requestId,
            @Valid @RequestBody RequestActionCreateRequest createRequest) {
        RequestActionResponse action = requestActionService.createAction(requestId, createRequest);
        return new ResponseEntity<>(action, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RequestActionResponse>> getActions(@PathVariable Long requestId) {
        List<RequestActionResponse> actions = requestActionService.getActions(requestId);
        return ResponseEntity.ok(actions);
    }
}
