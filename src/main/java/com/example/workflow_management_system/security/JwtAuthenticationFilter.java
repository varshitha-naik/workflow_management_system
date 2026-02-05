package com.example.workflow_management_system.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            logger.info("Incoming request URI: {}", request.getRequestURI());

            String authHeader = request.getHeader("Authorization");
            logger.info("Authorization header: {}", authHeader);

            String token = getTokenFromRequest(request);
            logger.info("Token extracted: {}", (token != null));

            if (StringUtils.hasText(token)) {
                boolean isValid = jwtTokenProvider.validateToken(token);
                logger.info("Token validation result: {}", isValid);

                if (isValid) {
                    String username = jwtTokenProvider.getUsername(token);
                    logger.info("Extracted username: {}", username);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());

                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.clearContext();
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    // Set TenantContext
                    if (userDetails instanceof UserPrincipal) {
                        UserPrincipal principal = (UserPrincipal) userDetails;
                        if (principal.getTenantId() != null) {
                            TenantContext.setTenantId(principal.getTenantId());
                            logger.info("TenantContext set to: {}", principal.getTenantId());
                        }
                    }

                    logger.info("SecurityContext set for user: {} with authorities: {}", username,
                            userDetails.getAuthorities());
                }
            } else {
                logger.info("No token found or token extraction failed");
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage(), e);
            // Even if auth fails, we should let filter chain proceed or return 401.
            // Following original logic, we call chain, but arguably if exception it might
            // stop.
            // But usually we should rethrow or handle.
            // Original code didn't clear context in finally.
            throw new ServletException("Authentication failed", e); // Ensure exception propagates if needed, or handle
                                                                    // delicately
        } finally {
            TenantContext.clear();
        }
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
