package com.example.workflow_management_system.repository;

import com.example.workflow_management_system.model.RefreshToken;
import com.example.workflow_management_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM refreshtoken r WHERE r.user.id = :userId")
    int deleteByUserId(Long userId);
}
