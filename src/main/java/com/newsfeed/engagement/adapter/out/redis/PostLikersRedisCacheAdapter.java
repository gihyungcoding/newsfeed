package com.newsfeed.engagement.adapter.out.redis;

import com.newsfeed.common.cache.JitteredTtl;
import com.newsfeed.common.cache.RedisKeys;
import com.newsfeed.engagement.application.port.out.PostLikersCachePort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
class PostLikersRedisCacheAdapter implements PostLikersCachePort {

    private static final Duration BASE_TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    PostLikersRedisCacheAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void add(long postId, long userId) {
        String key = RedisKeys.postLikers(postId);
        redisTemplate.opsForSet().add(key, String.valueOf(userId));
        redisTemplate.expire(key, JitteredTtl.of(BASE_TTL, 0.1));
    }

    @Override
    public void remove(long postId, long userId) {
        redisTemplate.opsForSet().remove(RedisKeys.postLikers(postId), String.valueOf(userId));
    }

    @Override
    public boolean exists(long postId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.postLikers(postId)));
    }

    @Override
    public boolean isMember(long postId, long userId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(RedisKeys.postLikers(postId), String.valueOf(userId)));
    }
}
