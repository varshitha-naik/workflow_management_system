package com.example.workflow_management_system.controller;

import com.example.workflow_management_system.dto.AuthResponse;
import com.example.workflow_management_system.dto.LoginRequest;
import com.example.workflow_management_system.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {

        private final AuthenticationManager authenticationManager;
        private final JwtTokenProvider jwtTokenProvider;
        private final com.example.workflow_management_system.service.RefreshTokenService refreshTokenService;
        private final com.example.workflow_management_system.service.UserService userService;
        private final com.example.workflow_management_system.service.PasswordResetService passwordResetService;

        public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider,
                        com.example.workflow_management_system.service.RefreshTokenService refreshTokenService,
                        com.example.workflow_management_system.service.UserService userService,
                        com.example.workflow_management_system.service.PasswordResetService passwordResetService) {
                this.authenticationManager = authenticationManager;
                this.jwtTokenProvider = jwtTokenProvider;
                this.refreshTokenService = refreshTokenService;
                this.userService = userService;
                this.passwordResetService = passwordResetService;
        }

        @PostMapping("/login")
        @Operation(summary = "User Login", description = "Authenticates a user and returns a JWT token. Returns 401 if credentials are invalid.")
        public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
                Authentication authentication = authenticationManager
                                .authenticate(new UsernamePasswordAuthenticationToken(
                                                loginRequest.usernameOrEmail(),
                                                loginRequest.password()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                String token = jwtTokenProvider.generateToken(authentication);

                com.example.workflow_management_system.security.UserPrincipal userDetails = (com.example.workflow_management_system.security.UserPrincipal) authentication
                                .getPrincipal();

                com.example.workflow_management_system.model.RefreshToken refreshToken = refreshTokenService
                                .createRefreshTokenForUser(userDetails.getId());

                return ResponseEntity.ok(new AuthResponse(token, refreshToken.getToken()));
        }

        @PostMapping("/refresh")
        public ResponseEntity<com.example.workflow_management_system.dto.TokenRefreshResponse> refresh(
                        @Valid @RequestBody com.example.workflow_management_system.dto.RefreshTokenRequest request) {
                String requestRefreshToken = request.refreshToken();

                return refreshTokenService.findByToken(requestRefreshToken)
                                .map(refreshTokenService::verifyExpiration)
                                .map(com.example.workflow_management_system.model.RefreshToken::getUser)
                                .map(user -> {
                                        String token = jwtTokenProvider
                                                        .generateToken(new UsernamePasswordAuthenticationToken(
                                                                        com.example.workflow_management_system.security.UserPrincipal
                                                                                        .create(user),
                                                                        null));
                                        return ResponseEntity
                                                        .ok(new com.example.workflow_management_system.dto.TokenRefreshResponse(
                                                                        token));
                                })
                                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                                                org.springframework.http.HttpStatus.UNAUTHORIZED,
                                                "Refresh token is not in database!"));
        }

        @PostMapping("/set-password")
        public ResponseEntity<Void> setPassword(
                        @Valid @RequestBody com.example.workflow_management_system.dto.SetPasswordRequest request) {
                userService.setPassword(request);
                return ResponseEntity.ok().build();
        }

        @PostMapping("/forgot-password")
        public ResponseEntity<Void> forgotPassword(
                        @Valid @RequestBody com.example.workflow_management_system.dto.ForgotPasswordRequest request) {
                passwordResetService.forgotPassword(request);
                return ResponseEntity.ok().build();
        }

        @PostMapping("/reset-password")
        public ResponseEntity<Void> resetPassword(
                        @Valid @RequestBody com.example.workflow_management_system.dto.ResetPasswordRequest request) {
                passwordResetService.resetPassword(request);
                return ResponseEntity.ok().build();
        }
}
