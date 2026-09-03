package com.zb.jogakjogak.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenRedisService {

    private static final String REFRESH_PREFIX = "refresh:";
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L;

    private final RedisTemplate<String, String> redisTemplate;

    public void save(Long userId, String jti) {
        redisTemplate.opsForValue().set(key(userId, jti), "1", REFRESH_TOKEN_EXPIRATION_MS, TimeUnit.MILLISECONDS);
    }

    public boolean exists(Long userId, String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(userId, jti)));
    }

    public void revoke(Long userId, String jti) {
        redisTemplate.delete(key(userId, jti));
    }

    public void revokeAll(Long userId) {
        Set<String> keys = redisTemplate.keys(REFRESH_PREFIX + userId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String key(Long userId, String jti) {
        return REFRESH_PREFIX + userId + ":" + jti;
    }
}
