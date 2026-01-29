package com.example.workflow_management_system.aspect;

import com.example.workflow_management_system.model.IdempotencyKey;
import com.example.workflow_management_system.repository.IdempotencyKeyRepository;
import com.example.workflow_management_system.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

@Aspect
@Component
public class IdempotencyAspect {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyAspect(IdempotencyKeyRepository idempotencyKeyRepository, ObjectMapper objectMapper) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(Idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attributes.getRequest();
        String idempotencyKeyHeader = request.getHeader("Idempotency-Key");

        if (idempotencyKeyHeader == null || idempotencyKeyHeader.isBlank()) {
            return joinPoint.proceed();
        }

        Long tenantId = SecurityUtils.getCurrentTenantId();
        String key = idempotencyKeyHeader;
        String requestPath = request.getRequestURI();

        // Calculate request checksum
        // We assume the body is the first argument or one of arguments
        // However, extracting request body from JoinPoint arguments is safer than
        // re-reading servlet stream
        Object[] args = joinPoint.getArgs();
        String requestChecksum = calculateChecksum(request, args);

        // Check for existing key
        // Transaction: This read is part of whatever transaction we are in (if any), or
        // none.
        // Aspect precedence matters. Usually Transactional wraps Aspect.
        Optional<IdempotencyKey> existingKeyOpt = idempotencyKeyRepository.findByKeyAndTenantId(key, tenantId);

        if (existingKeyOpt.isPresent()) {
            IdempotencyKey existingKey = existingKeyOpt.get();

            // Check checksum
            if (!existingKey.getRequestChecksum().equals(requestChecksum)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency key reused with different request payload");
            }

            // Return stored response
            Class<?> returnType = getReturnTypeClass(joinPoint);
            Object body = null;
            if (existingKey.getResponseBody() != null && !existingKey.getResponseBody().equals("null")) {
                body = objectMapper.readValue(existingKey.getResponseBody(), returnType);
            }

            return ResponseEntity.status(existingKey.getResponseStatus()).body(body);
        }

        // Proceed and execute logic
        Object result = joinPoint.proceed();

        // Save result
        if (result instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
            int status = responseEntity.getStatusCode().value();
            Object body = responseEntity.getBody();
            String responseBodyJson = objectMapper.writeValueAsString(body);

            try {
                // Determine if we are in a transaction. Controller methods often call Service
                // methods which are Transactional.
                // The Controller itself is not usually Transactional.
                // We want to save the key NOW.
                // But if the Service rolled back, the Controller might throw Exception instead
                // of returning ResponseEntity (handled by ExceptionHandler).
                // If Exception was thrown, we are NOT here (since proceed threw).
                // So if we are here, execution was successful.

                IdempotencyKey newKey = new IdempotencyKey(key, tenantId, requestPath, requestChecksum, status,
                        responseBodyJson);
                idempotencyKeyRepository.save(newKey);
            } catch (Exception e) {
                // If saving fails (e.g. race condition unique constraint), we should probably
                // fail?
                // Or try to return the result anyway?
                // If T2 fails to save key, it executed logic.
                // We should ideally return the result. The client will get a result.
                // Next retry will fail logic (presumably) or succeed idempotently if key was
                // saved by T1?
                // We log and ignore save failure to prefer availability?
                // Requirement says "Thread-safe (transactional)".
                // If we fail to save the key, the idempotency guarantee for FUTURE requests is
                // lost (or relies on T1).
                // But if we throw, client thinks logic failed? But logic succeeded.
                // Safest to return the result even if key save fails (due to race).
                // Usually "DataIntegrityViolationException" if duplicate.
                org.slf4j.LoggerFactory.getLogger(IdempotencyAspect.class).warn("Failed to save idempotency key", e);
            }
        }

        return result;
    }

    private String calculateChecksum(HttpServletRequest request, Object[] args) {
        try {
            // Include method and path in checksum to ensure uniqueness across different
            // endpoints
            // Example format: POST:/api/requests/{id}/actions:<serialized-json-body>
            String method = request.getMethod();
            String path = request.getRequestURI();
            String bodyJson = objectMapper.writeValueAsString(args);

            String input = method + ":" + path + ":" + bodyJson;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (Exception e) {
            return "unknown"; // Should not happen
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private Class<?> getReturnTypeClass(ProceedingJoinPoint joinPoint) {
        org.aspectj.lang.reflect.MethodSignature signature = (org.aspectj.lang.reflect.MethodSignature) joinPoint
                .getSignature();
        java.lang.reflect.Type genericReturnType = signature.getMethod().getGenericReturnType();

        // Handle ResponseEntity<Type>
        if (genericReturnType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) genericReturnType;
            if (pt.getRawType().equals(ResponseEntity.class)) {
                java.lang.reflect.Type actualType = pt.getActualTypeArguments()[0];
                if (actualType instanceof Class) {
                    return (Class<?>) actualType;
                }
            }
        }
        return Object.class; // Fallback
    }

    // Helper needed for reading ResponseEntity body type?
    // Actually, objectmapper.readValue(json, Object.class) might return
    // LinkedHashMap.
    // That works for serialization back to JSON in ResponseEntity.
}
