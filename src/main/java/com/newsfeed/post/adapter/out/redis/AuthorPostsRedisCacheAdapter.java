package com.newsfeed.post.adapter.out.redis;

import com.newsfeed.common.cache.JitteredTtl;
import com.newsfeed.common.cache.RedisKeys;
import com.newsfeed.post.application.port.out.AuthorPostsCachePort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;

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

    @Override
    public List<Long> recentPostIds(long authorId, int limit) {
        // rank 기준 상위 limit개(= score, 즉 작성시각이 가장 큰 쪽) — 순서는 호출자가 병합 시 다시 정렬한다
        Set<String> members = redisTemplate.opsForZSet().reverseRange(RedisKeys.authorPosts(authorId), 0, limit - 1);
        if (members == null) {
            return List.of();
        }
        return members.stream().map(Long::parseLong).toList();
    }
}
