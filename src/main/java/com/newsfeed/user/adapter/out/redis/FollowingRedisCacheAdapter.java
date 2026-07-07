package com.newsfeed.user.adapter.out.redis;

import com.newsfeed.common.cache.RedisKeys;
import com.newsfeed.user.application.port.out.FollowingCachePort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** 소셜 그래프 캐시 (following:{userId}, Set). 지금은 무효화만, 조회는 5단계(feed)에서 추가. */
@Component
class FollowingRedisCacheAdapter implements FollowingCachePort {

    private final StringRedisTemplate redisTemplate;

    FollowingRedisCacheAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void evict(long userId) {
        redisTemplate.delete(RedisKeys.following(userId));
    }
}
