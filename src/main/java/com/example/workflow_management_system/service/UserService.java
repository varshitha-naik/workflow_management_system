package com.example.workflow_management_system.service;

import com.example.workflow_management_system.dto.UserRequest;
import com.example.workflow_management_system.dto.UserResponse;
import com.example.workflow_management_system.model.Tenant;
import com.example.workflow_management_system.model.User;
import com.example.workflow_management_system.repository.TenantRepository;
import com.example.workflow_management_system.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.example.workflow_management_system.repository.InviteTokenRepository inviteTokenRepository;
    private final MailService mailService;

    public UserService(UserRepository userRepository, TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder,
            com.example.workflow_management_system.repository.InviteTokenRepository inviteTokenRepository,
            MailService mailService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.inviteTokenRepository = inviteTokenRepository;
        this.mailService = mailService;
    }

    public UserResponse createUser(UserRequest request) {
        // Enforce Tenant Isolation
        // com.example.workflow_management_system.security.SecurityUtils.validateTenantAccess(request.tenantId());
        Long tenantId = com.example.workflow_management_system.security.SecurityUtils.getCurrentUser().getTenantId();

        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        // Validate Role Hierarchy
        com.example.workflow_management_system.security.UserPrincipal currentUser = com.example.workflow_management_system.security.SecurityUtils
                .getCurrentUser();
        String currentRoleName = currentUser.getRole();
        com.example.workflow_management_system.model.UserRole requestRole = request.role();

        if ("ADMIN".equals(currentRoleName)) {
            if (requestRole != com.example.workflow_management_system.model.UserRole.USER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMINs can only create USERs.");
            }
        } else if ("SUPER_ADMIN".equals(currentRoleName)) {
            if (requestRole == com.example.workflow_management_system.model.UserRole.SUPER_ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot create another SUPER_ADMIN.");
            }
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant not found"));

        User user = new User(
                request.username(),
                request.email(),
                null, // Password is null until set by user via invite
                request.role(),
                tenant,
                false);

        User savedUser = userRepository.save(user);

        // Create Invite Token
        com.example.workflow_management_system.model.InviteToken inviteToken = new com.example.workflow_management_system.model.InviteToken(
                java.util.UUID.randomUUID().toString(),
                savedUser,
                java.time.LocalDateTime.now().plusHours(24) // 24 hours expiry
        );
        inviteTokenRepository.save(inviteToken);

        // Send invite email with token
        mailService.sendInvitationEmail(savedUser.getEmail(), savedUser.getUsername(), tenant.getName(),
                inviteToken.getToken());

        return mapToResponse(savedUser);
    }

    public UserResponse updateUser(Long id, UserRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Enforce Cross-Tenant Data Access Check on existing user
        com.example.workflow_management_system.security.SecurityUtils.validateTenantAccess(user.getTenant().getId());

        // Validate Role Hierarchy for Update
        com.example.workflow_management_system.security.UserPrincipal currentUser = com.example.workflow_management_system.security.SecurityUtils
                .getCurrentUser();
        String currentRoleName = currentUser.getRole();
        com.example.workflow_management_system.model.UserRole targetRole = user.getRole();

        if ("ADMIN".equals(currentRoleName)) {
            // ADMIN cannot edit ADMIN or SUPER_ADMIN
            if (targetRole == com.example.workflow_management_system.model.UserRole.ADMIN
                    || targetRole == com.example.workflow_management_system.model.UserRole.SUPER_ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "ADMINs cannot edit ADMIN or SUPER_ADMIN accounts.");
            }
            // ADMIN cannot promote someone to ADMIN or SUPER_ADMIN
            if (request.role() != com.example.workflow_management_system.model.UserRole.USER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMINs can only set role to USER.");
            }
        } else if ("SUPER_ADMIN".equals(currentRoleName)) {
            // SUPER_ADMIN cannot update role to SUPER_ADMIN
            if (request.role() == com.example.workflow_management_system.model.UserRole.SUPER_ADMIN
                    && targetRole != com.example.workflow_management_system.model.UserRole.SUPER_ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot promote to SUPER_ADMIN.");
            }
        }

        // Check uniqueness logic if username/email changed
        if (!user.getUsername().equals(request.username()) && userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setRole(request.role());
        // user.setTenant(tenant); // Tenant cannot be changed via update
        user.setActive(request.active());

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        com.example.workflow_management_system.security.SecurityUtils.validateTenantAccess(user.getTenant().getId());

        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByTenant(Long tenantId) {
        com.example.workflow_management_system.security.SecurityUtils.validateTenantAccess(tenantId);

        if (!tenantRepository.existsById(tenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
        }
        return userRepository.findByTenant_Id(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse updateUserStatus(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        com.example.workflow_management_system.security.SecurityUtils.validateTenantAccess(user.getTenant().getId());

        // Role check for status update
        com.example.workflow_management_system.security.UserPrincipal currentUser = com.example.workflow_management_system.security.SecurityUtils
                .getCurrentUser();
        if ("ADMIN".equals(currentUser.getRole())) {
            if (user.getRole() == com.example.workflow_management_system.model.UserRole.ADMIN
                    || user.getRole() == com.example.workflow_management_system.model.UserRole.SUPER_ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMINs cannot change status of higher roles.");
            }
        }

        user.setActive(active);
        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getTenant().getId(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    @Transactional
    public void setPassword(com.example.workflow_management_system.dto.SetPasswordRequest request) {
        com.example.workflow_management_system.model.InviteToken inviteToken = inviteTokenRepository
                .findByToken(request.token())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));

        if (inviteToken.isUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token already used");
        }

        if (inviteToken.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expired");
        }

        User user = inviteToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setActive(true);
        userRepository.save(user);

        inviteToken.setUsed(true);
        inviteTokenRepository.save(inviteToken);
    }

    public UserResponse updateProfile(Long userId,
            com.example.workflow_management_system.dto.UserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.getUsername().equals(request.username()) && userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        user.setUsername(request.username());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        return mapToResponse(userRepository.save(user));
    }
}
