package com.zb.jogakjogak.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenRedisService {

    private static final String REFRESH_PREFIX = "refresh:";
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L;

    private final RedisTemplate<String, String> redisTemplate;

    public void save(Long userId, String token) {
        redisTemplate.opsForValue().set(REFRESH_PREFIX + userId, token, REFRESH_TOKEN_EXPIRATION_MS, TimeUnit.MILLISECONDS);
    }

    public Optional<String> get(Long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(REFRESH_PREFIX + userId));
    }

    public void delete(Long userId) {
        redisTemplate.delete(REFRESH_PREFIX + userId);
    }
}
