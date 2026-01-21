package com.example.workflow_management_system.service;

import com.example.workflow_management_system.dto.TenantRequest;
import com.example.workflow_management_system.dto.TenantResponse;
import com.example.workflow_management_system.model.Tenant;
import com.example.workflow_management_system.model.TenantStatus;
import com.example.workflow_management_system.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;
    private final com.example.workflow_management_system.repository.UserRepository userRepository;
    private final com.example.workflow_management_system.repository.InviteTokenRepository inviteTokenRepository;
    private final MailService mailService;

    public TenantService(TenantRepository tenantRepository,
            com.example.workflow_management_system.repository.UserRepository userRepository,
            com.example.workflow_management_system.repository.InviteTokenRepository inviteTokenRepository,
            MailService mailService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.inviteTokenRepository = inviteTokenRepository;
        this.mailService = mailService;
    }

    public TenantResponse createTenant(com.example.workflow_management_system.dto.TenantCreateRequest request) {
        if (tenantRepository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tenant with name '" + request.name() + "' already exists");
        }

        if (userRepository.existsByUsername(request.adminUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (userRepository.existsByEmail(request.adminEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        Tenant tenant = new Tenant(request.name(), TenantStatus.ACTIVE);
        Tenant savedTenant = tenantRepository.save(tenant);

        com.example.workflow_management_system.model.User adminUser = new com.example.workflow_management_system.model.User(
                request.adminUsername(),
                request.adminEmail(),
                null, // Password is null until set by user via invite
                com.example.workflow_management_system.model.UserRole.SUPER_ADMIN,
                savedTenant,
                true);
        com.example.workflow_management_system.model.User savedUser = userRepository.save(adminUser);

        // Create Invite Token for SUPER_ADMIN
        com.example.workflow_management_system.model.InviteToken inviteToken = new com.example.workflow_management_system.model.InviteToken(
                java.util.UUID.randomUUID().toString(),
                savedUser,
                java.time.LocalDateTime.now().plusHours(24) // 24 hours expiry
        );
        inviteTokenRepository.save(inviteToken);

        // Send invite email with token
        mailService.sendInvitationEmail(savedUser.getEmail(), savedUser.getUsername(), savedTenant.getName(),
                inviteToken.getToken());

        return mapToResponse(savedTenant);
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> getAllTenants() {
        // Strict Isolation: A user can ONLY see their own tenant
        Long currentTenantId = com.example.workflow_management_system.security.SecurityUtils.getCurrentTenantId();
        return tenantRepository.findAll().stream()
                .filter(tenant -> tenant.getId().equals(currentTenantId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TenantResponse getTenantById(Long id) {
        com.example.workflow_management_system.security.SecurityUtils.validateTenantAccess(id);

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found with id: " + id));
        return mapToResponse(tenant);
    }

    public TenantResponse updateTenant(Long id, TenantRequest request) {
        com.example.workflow_management_system.security.SecurityUtils.validateTenantAccess(id);

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found with id: " + id));

        // Check if name is taken by another tenant
        tenantRepository.findByName(request.name()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Tenant with name '" + request.name() + "' already exists");
            }
        });

        tenant.setName(request.name());
        Tenant updatedTenant = tenantRepository.save(tenant);
        return mapToResponse(updatedTenant);
    }

    public void deleteTenant(Long id) {
        com.example.workflow_management_system.security.SecurityUtils.validateTenantAccess(id);

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found with id: " + id));

        tenant.setStatus(TenantStatus.INACTIVE);
        tenantRepository.save(tenant);
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getStatus(),
                tenant.getCreatedAt());
    }
}
