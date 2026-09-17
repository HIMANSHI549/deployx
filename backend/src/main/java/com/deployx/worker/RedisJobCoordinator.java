package com.deployx.worker;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisJobCoordinator {

    private final StringRedisTemplate redis;
    private final boolean enabled;
    private final Duration lockDuration;

    public RedisJobCoordinator(
            StringRedisTemplate redis,
            @Value("${deployx.coordination.enabled:false}") boolean enabled,
            @Value("${deployx.coordination.lock-seconds:900}") long lockSeconds) {
        this.redis = redis;
        this.enabled = enabled;
        this.lockDuration = Duration.ofSeconds(lockSeconds);
    }

    public boolean tryStart(UUID deploymentId) {
        if (!enabled) {
            return true;
        }
        Boolean acquired = redis.opsForValue().setIfAbsent(
                "deployx:deployment:lock:" + deploymentId,
                "running",
                lockDuration);
        return Boolean.TRUE.equals(acquired);
    }

    public void mark(String deploymentId, String state) {
        if (enabled) {
            redis.opsForValue().set("deployx:deployment:state:" + deploymentId, state, lockDuration);
        }
    }
}