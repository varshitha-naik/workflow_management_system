package com.example.workflow_management_system.service;

import com.example.workflow_management_system.model.RefreshToken;
import com.example.workflow_management_system.repository.RefreshTokenRepository;
import com.example.workflow_management_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    @Value("${app.jwt-refresh-expiration-milliseconds}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken createRefreshToken(Long userId) {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")));
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());

        // Handle existing token for user to enforce 1 per user, optional but simpler
        // For now just save new one, but unique constraint on user might be good or
        // delete old
        // But since we didn't put unique on user column in entity (only OneToOne
        // implies it if modeled right,
        // but JPA allows multiple unless unique=true), let's delete old one first to be
        // safe and clean.
        // Actually OneToOne usually implies 1-to-1 db relationship if constraints are
        // set.
        // Let's explicitly delete previous tokens for user.
        // Since deleteByUser is transactional, we need @Transactional on the method or
        // class
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken createRefreshTokenForUser(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        refreshTokenRepository.flush(); // Force delete to DB
        return createRefreshToken(userId);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Transactional
    public int deleteByUserId(Long userId) {
        return refreshTokenRepository.deleteByUserId(userId);
    }
}
