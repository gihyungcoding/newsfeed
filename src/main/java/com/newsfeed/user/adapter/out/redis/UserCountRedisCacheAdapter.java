package com.newsfeed.user.adapter.out.redis;

import com.newsfeed.user.application.port.out.UserCountCachePort;
import com.newsfeed.user.domain.FollowCounts;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/** 횟수 캐시 (cnt:user:{id}, Hash {followers, following}). */
@Component
class UserCountRedisCacheAdapter implements UserCountCachePort {

    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    UserCountRedisCacheAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<FollowCounts> find(long userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key(userId));
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new FollowCounts(
                Integer.parseInt((String) entries.get("followers")),
                Integer.parseInt((String) entries.get("following"))));
    }

    @Override
    public void save(long userId, FollowCounts counts) {
        String key = key(userId);
        redisTemplate.opsForHash().putAll(key, Map.of(
                "followers", String.valueOf(counts.followerCount()),
                "following", String.valueOf(counts.followingCount())));
        redisTemplate.expire(key, TTL);
    }

    @Override
    public void incrementIfPresent(long userId, int followerDelta, int followingDelta) {
        String key = key(userId);
        // EXISTS 확인과 HINCRBY 사이에 TTL 만료가 끼어들 수 있지만,
        // 그 경우 다음 조회 miss 때 DB 값으로 재적재되므로 오차는 자가 치유된다 (§3.2)
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return;
        }
        if (followerDelta != 0) {
            redisTemplate.opsForHash().increment(key, "followers", followerDelta);
        }
        if (followingDelta != 0) {
            redisTemplate.opsForHash().increment(key, "following", followingDelta);
        }
    }

    private String key(long userId) {
        return "cnt:user:" + userId;
    }
}
