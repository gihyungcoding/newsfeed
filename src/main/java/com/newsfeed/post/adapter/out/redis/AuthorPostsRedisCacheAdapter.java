package com.newsfeed.post.adapter.out.redis;

import com.newsfeed.common.cache.JitteredTtl;
import com.newsfeed.common.cache.RedisKeys;
import com.newsfeed.post.application.port.out.AuthorPostsCachePort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 작성자별 최근 포스트 캐시 (author-posts:{authorId}, Sorted Set).
 * 최근 100개로 상한을 둔다 — celebrity pull 경로는 최근 포스트 몇 개만 보면 충분하다.
 */
@Component
class AuthorPostsRedisCacheAdapter implements AuthorPostsCachePort {

    private static final int MAX_SIZE = 100;
    private static final Duration BASE_TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    AuthorPostsRedisCacheAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void push(long authorId, long postId, long createdAtEpochMillis) {
        String key = RedisKeys.authorPosts(authorId);
        redisTemplate.opsForZSet().add(key, String.valueOf(postId), createdAtEpochMillis);
        redisTemplate.opsForZSet().removeRange(key, 0, -(MAX_SIZE + 1));
        redisTemplate.expire(key, JitteredTtl.of(BASE_TTL, 0.1));
    }
}
