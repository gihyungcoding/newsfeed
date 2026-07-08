package com.newsfeed.post.adapter.out.redis;

import com.newsfeed.common.cache.JitteredTtl;
import com.newsfeed.common.cache.RedisKeys;
import com.newsfeed.post.application.port.out.PostCountCachePort;
import com.newsfeed.post.domain.PostCounts;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/** 횟수 캐시 (cnt:post:{id}, Hash {likes, replies}). user 컨텍스트의 UserCountRedisCacheAdapter와 같은 패턴. */
@Component
class PostCountRedisCacheAdapter implements PostCountCachePort {

    private static final Duration BASE_TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    PostCountRedisCacheAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<PostCounts> find(long postId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(RedisKeys.postCounts(postId));
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PostCounts(
                Integer.parseInt((String) entries.get("likes")),
                Integer.parseInt((String) entries.get("replies"))));
    }

    @Override
    public void save(long postId, PostCounts counts) {
        String key = RedisKeys.postCounts(postId);
        redisTemplate.opsForHash().putAll(key, Map.of(
                "likes", String.valueOf(counts.likeCount()),
                "replies", String.valueOf(counts.replyCount())));
        redisTemplate.expire(key, JitteredTtl.of(BASE_TTL, 0.1));
    }

    @Override
    public void incrementIfPresent(long postId, int likeDelta, int replyDelta) {
        String key = RedisKeys.postCounts(postId);
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return;
        }
        if (likeDelta != 0) {
            redisTemplate.opsForHash().increment(key, "likes", likeDelta);
        }
        if (replyDelta != 0) {
            redisTemplate.opsForHash().increment(key, "replies", replyDelta);
        }
    }
}
