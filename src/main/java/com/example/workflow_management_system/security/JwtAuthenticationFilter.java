package com.example.workflow_management_system.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

        logger.info("LIVE_TRACE: JwtAuthenticationFilter - Entering doFilterInternal for URI: {}",
                request.getRequestURI());

        try {
            // 5. Verify token parsing
            String header = request.getHeader("Authorization");
            logger.info("LIVE_TRACE: JwtAuthenticationFilter - Authorization Header: {}", header);

            String token = getTokenFromRequest(request);
            logger.info("LIVE_TRACE: JwtAuthenticationFilter - Token extracted: {}",
                    (token != null ? "YES (Length: " + token.length() + ")" : "NO"));

            if (StringUtils.hasText(token)) {
                boolean isValid = false;
                try {
                    isValid = jwtTokenProvider.validateToken(token);
                } catch (Exception e) {
                    logger.error("LIVE_TRACE: JwtAuthenticationFilter - Validation Exception: ", e);
                }

                logger.info("LIVE_TRACE: JwtAuthenticationFilter - Token Valid: {}", isValid);

                if (isValid) {
                    String username = jwtTokenProvider.getUsername(token);
                    logger.info("LIVE_TRACE: JwtAuthenticationFilter - Username from token: {}", username);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    logger.info("LIVE_TRACE: JwtAuthenticationFilter - UserDetails Loaded: {}",
                            userDetails.getUsername());
                    logger.info("LIVE_TRACE: JwtAuthenticationFilter - Authorities from UserDetails: {}",
                            userDetails.getAuthorities());

                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());

                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.clearContext();
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    // 6. Verify AuthenticationContext is set
                    logger.info("LIVE_TRACE: JwtAuthenticationFilter - SecurityContext Updated. Principal: {}",
                            SecurityContextHolder.getContext().getAuthentication().getName());
                    logger.info("LIVE_TRACE: JwtAuthenticationFilter - Context Authorities: {}",
                            SecurityContextHolder.getContext().getAuthentication().getAuthorities());

                    // Set TenantContext
                    if (userDetails instanceof UserPrincipal) {
                        UserPrincipal principal = (UserPrincipal) userDetails;
                        if (principal.getTenantId() != null) {
                            TenantContext.setTenantId(principal.getTenantId());
                            logger.info("LIVE_TRACE: JwtAuthenticationFilter - TenantContext SET: {}",
                                    principal.getTenantId());
                        } else {
                            logger.info(
                                    "LIVE_TRACE: JwtAuthenticationFilter - TenantContext: GLOBAL USER (No Tenant ID)");
                            TenantContext.clear(); // Explicitly clear to be safe
                        }
                    }
                } else {
                    logger.warn("LIVE_TRACE: JwtAuthenticationFilter - Token INVALID");
                }
            } else {
                logger.info("LIVE_TRACE: JwtAuthenticationFilter - No Token Found in request");
            }
            logger.info("================ AUTHENTICATION FILTER END ================");

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage(), e);
            throw new ServletException("Authentication failed", e);
        } finally {
            TenantContext.clear();
        }
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.toLowerCase().startsWith("bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
