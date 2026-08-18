package com.sirh.security;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Blacklist JWT sur Redis (clé blacklist:{jti}, TTL = durée de vie restante du token) — cf.
 * ARCHITECTURE.md §7. Utilisée au logout pour révoquer immédiatement un token encore valide.
 */
@Service
public class TokenBlacklistService {

  private static final String KEY_PREFIX = "blacklist:";

  private final StringRedisTemplate redisTemplate;

  public TokenBlacklistService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void blacklist(String jti, long ttlSeconds) {
    if (ttlSeconds <= 0) {
      return;
    }
    redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
  }

  public boolean isBlacklisted(String jti) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
  }
}
