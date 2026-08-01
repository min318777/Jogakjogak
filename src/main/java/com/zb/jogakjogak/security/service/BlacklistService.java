package com.zb.jogakjogak.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private final RedisTemplate<String, String> redisTemplate;

    public void addToBlacklist(String jti, long remainingMs) {
        if (remainingMs > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "logout", remainingMs, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }
}
