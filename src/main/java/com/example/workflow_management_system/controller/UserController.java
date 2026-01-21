package com.example.workflow_management_system.controller;

import com.example.workflow_management_system.dto.UserRequest;
import com.example.workflow_management_system.dto.UserResponse;
import com.example.workflow_management_system.dto.UserStatusRequest;
import com.example.workflow_management_system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.createUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<java.util.List<UserResponse>> getAllUsers() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        com.example.workflow_management_system.security.UserPrincipal currentUser = (com.example.workflow_management_system.security.UserPrincipal) authentication
                .getPrincipal();
        return ResponseEntity.ok(userService.getUsersByTenant(currentUser.getTenantId()));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @GetMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PatchMapping("/{id}/status")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> updateUserStatus(@PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request) {
        return ResponseEntity.ok(userService.updateUserStatus(id, request.active()));
    }
}
