package com.example.workflow_management_system.controller;

import com.example.workflow_management_system.dto.RequestCreateRequest;
import com.example.workflow_management_system.dto.RequestResponse;
import com.example.workflow_management_system.service.RequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    public ResponseEntity<RequestResponse> createRequest(@RequestBody RequestCreateRequest request) {
        RequestResponse createdRequest = requestService.createRequest(request);
        return new ResponseEntity<>(createdRequest, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RequestResponse>> getAllRequests() {
        List<RequestResponse> requests = requestService.getAllRequests();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequestResponse> getRequest(@PathVariable Long id) {
        RequestResponse request = requestService.getRequest(id);
        return ResponseEntity.ok(request);
    }
}
