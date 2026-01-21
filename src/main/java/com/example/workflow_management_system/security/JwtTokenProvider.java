package com.example.workflow_management_system.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt-secret}")
    private String jwtSecret;

    @Value("${app.jwt-expiration-milliseconds}")
    private long jwtExpirationDate;

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + jwtExpirationDate);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        System.out.println("Generating Token for: " + username + " with Role: " + userPrincipal.getRole());

        return Jwts.builder()
                .setSubject(username)
                .claim("role", userPrincipal.getRole())
                .claim("tenantId", userPrincipal.getTenantId())
                .claim("tenantName", userPrincipal.getTenantName())
                .setIssuedAt(currentDate)
                .setExpiration(expireDate)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key key() {
        return Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(jwtSecret));
    }

    public String getUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Log exception
            System.err.println("Invalid JWT signature: " + e.getMessage());
        }
        return false;
    }
}
