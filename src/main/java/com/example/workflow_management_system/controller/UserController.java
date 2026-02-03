package com.example.workflow_management_system.controller;

import com.example.workflow_management_system.dto.UserRequest;
import com.example.workflow_management_system.dto.UserResponse;
import com.example.workflow_management_system.dto.UserStatusRequest;
import com.example.workflow_management_system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.workflow_management_system.security.UserPrincipal;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/users")
@Validated
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
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<org.springframework.data.domain.Page<UserResponse>> getAllUsers(
            @RequestParam(required = false) com.example.workflow_management_system.model.UserRole role,
            @RequestParam(required = false) Boolean active,
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        com.example.workflow_management_system.security.UserPrincipal currentUser = (com.example.workflow_management_system.security.UserPrincipal) authentication
                .getPrincipal();

        // Enforce implicit tenant scope for ALL roles including SUPER_ADMIN
        return ResponseEntity
                .ok(userService.getUsersByTenant(currentUser.getTenantId(), role, active, pageable));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable @Min(1) Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable @Min(1) Long id,
            @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @GetMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/me")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(userService.getUserById(userPrincipal.getId()));
    }

    @PatchMapping("/{id}/status")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> updateUserStatus(@PathVariable @Min(1) Long id,
            @Valid @RequestBody UserStatusRequest request) {
        return ResponseEntity.ok(userService.updateUserStatus(id, request.active()));
    }

    @PutMapping("/me")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<UserResponse> updateCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody com.example.workflow_management_system.dto.UserProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userPrincipal.getId(), request));
    }
}
