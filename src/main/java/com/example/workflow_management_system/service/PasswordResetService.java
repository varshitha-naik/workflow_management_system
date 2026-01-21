package com.example.workflow_management_system.service;

import com.example.workflow_management_system.dto.ForgotPasswordRequest;
import com.example.workflow_management_system.dto.ResetPasswordRequest;
import com.example.workflow_management_system.model.PasswordResetToken;
import com.example.workflow_management_system.model.User;
import com.example.workflow_management_system.repository.PasswordResetTokenRepository;
import com.example.workflow_management_system.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository, PasswordResetTokenRepository tokenRepository,
            MailService mailService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByUsernameOrEmail(request.email(), request.email()).ifPresent(user -> {
            tokenRepository.deleteByUser(user);
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken(token, user, LocalDateTime.now().plusHours(24));
            tokenRepository.save(resetToken);
            mailService.sendResetPasswordEmail(user.getEmail(), user.getUsername(), token);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.token())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));

        if (resetToken.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        tokenRepository.delete(resetToken);
    }
}
