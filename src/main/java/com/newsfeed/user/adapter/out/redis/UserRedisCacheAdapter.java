package com.newsfeed.user.adapter.out.redis;

import com.newsfeed.common.cache.JitteredTtl;
import com.newsfeed.common.cache.RedisKeys;
import com.newsfeed.user.application.port.out.UserCachePort;
import com.newsfeed.user.domain.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * 사용자 식별 정보 캐시 (user:{id}). JSON 직렬화 대신 Redis Hash를 사용한다
 * — 필드가 단순 문자열 몇 개라 직렬화 라이브러리 의존 없이 다룰 수 있다.
 */
@Component
class UserRedisCacheAdapter implements UserCachePort {

    private static final Duration BASE_TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    UserRedisCacheAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<User> find(long userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(RedisKeys.userProfile(userId));
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new User(
                userId,
                (String) entries.get("username"),
                (String) entries.get("displayName"),
                Instant.ofEpochMilli(Long.parseLong((String) entries.get("createdAt")))));
    }

    @Override
    public void save(User user) {
        String key = RedisKeys.userProfile(user.id());
        redisTemplate.opsForHash().putAll(key, Map.of(
                "username", user.username(),
                "displayName", user.displayName(),
                "createdAt", String.valueOf(user.createdAt().toEpochMilli())));
        // 지터를 둬서 동시에 적재된 프로필들이 한꺼번에 만료되지 않게 한다 (캐시 눈사태 예방)
        redisTemplate.expire(key, JitteredTtl.of(BASE_TTL, 0.1));
    }
}
