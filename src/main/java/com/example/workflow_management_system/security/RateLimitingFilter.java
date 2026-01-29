package com.example.workflow_management_system.security;

import com.example.workflow_management_system.service.RateLimitingService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;

    public RateLimitingFilter(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. Exclude Health / Swagger / Static Resources
        if (isExcluded(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Identify Key and Bucket Type
        String key;
        boolean isAuthEndpoint = path.startsWith("/api/v1/auth/");

        if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
                !(SecurityContextHolder.getContext()
                        .getAuthentication() instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            // Authenticated User
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserPrincipal) {
                UserPrincipal user = (UserPrincipal) principal;
                key = user.getTenantId() + ":" + user.getId();
            } else {
                // Fallback for unlikely case where principal is not UserPrincipal
                key = request.getRemoteAddr();
            }
        } else {
            // Unauthenticated - IP based
            key = request.getRemoteAddr();
        }

        // 3. Resolve Bucket
        Bucket bucket = rateLimitingService.resolveBucket(key, isAuthEndpoint);

        // 4. Try Consume
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.getWriter().write(
                    "{\"status\": 429, \"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded. Please try again in "
                            + waitForRefill + " seconds.\"}");
        }
    }

    private boolean isExcluded(String path) {
        return path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.equals("/favicon.ico") ||
                path.startsWith("/actuator/") ||
                path.startsWith("/swagger") ||
                path.startsWith("/v3/api-docs"); // Common patterns
    }
}
