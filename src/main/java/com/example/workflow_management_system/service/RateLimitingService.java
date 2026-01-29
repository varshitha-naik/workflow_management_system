package com.example.workflow_management_system.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key, boolean isAuthEndpoint) {
        return buckets.computeIfAbsent(key, k -> createNewBucket(isAuthEndpoint));
    }

    private Bucket createNewBucket(boolean isAuthEndpoint) {
        long capacity;
        Duration refillPeriod = Duration.ofMinutes(1);

        if (isAuthEndpoint) {
            capacity = 5;
        } else {
            capacity = 100;
        }

        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, refillPeriod));
        return Bucket.builder().addLimit(limit).build();
    }
}
