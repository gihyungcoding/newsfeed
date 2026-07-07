package com.newsfeed.post.adapter.out.redis;

import com.newsfeed.common.cache.JitteredTtl;
import com.newsfeed.common.cache.RedisKeys;
import com.newsfeed.post.application.port.out.PostCachePort;
import com.newsfeed.post.domain.Post;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/** 포스트 콘텐츠 캐시 (post:{id}, Hash). */
@Component
class PostRedisCacheAdapter implements PostCachePort {

    private static final Duration BASE_TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    PostRedisCacheAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<Post> find(long postId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(RedisKeys.post(postId));
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Post(
                postId,
                Long.parseLong((String) entries.get("authorId")),
                (String) entries.get("content"),
                Integer.parseInt((String) entries.get("likeCount")),
                Integer.parseInt((String) entries.get("replyCount")),
                Instant.ofEpochMilli(Long.parseLong((String) entries.get("createdAt")))));
    }

    @Override
    public void save(Post post) {
        String key = RedisKeys.post(post.id());
        redisTemplate.opsForHash().putAll(key, Map.of(
                "authorId", String.valueOf(post.authorId()),
                "content", post.content(),
                "likeCount", String.valueOf(post.likeCount()),
                "replyCount", String.valueOf(post.replyCount()),
                "createdAt", String.valueOf(post.createdAt().toEpochMilli())));
        redisTemplate.expire(key, JitteredTtl.of(BASE_TTL, 0.1));
    }
}
