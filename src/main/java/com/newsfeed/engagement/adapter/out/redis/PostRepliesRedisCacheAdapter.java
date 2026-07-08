package com.newsfeed.engagement.adapter.out.redis;

import com.newsfeed.common.cache.JitteredTtl;
import com.newsfeed.common.cache.RedisKeys;
import com.newsfeed.engagement.application.port.out.PostRepliesCachePort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/** post 컨텍스트의 AuthorPostsRedisCacheAdapter와 같은 패턴 (상한 100, TTL 1시간+지터). */
@Component
class PostRepliesRedisCacheAdapter implements PostRepliesCachePort {

    private static final int MAX_SIZE = 100;
    private static final Duration BASE_TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    PostRepliesRedisCacheAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void push(long postId, long replyId, long createdAtEpochMillis) {
        String key = RedisKeys.postReplies(postId);
        redisTemplate.opsForZSet().add(key, String.valueOf(replyId), createdAtEpochMillis);
        redisTemplate.opsForZSet().removeRange(key, 0, -(MAX_SIZE + 1));
        redisTemplate.expire(key, JitteredTtl.of(BASE_TTL, 0.1));
    }

    @Override
    public List<Long> recentReplyIds(long postId, int limit) {
        Set<String> members = redisTemplate.opsForZSet().reverseRange(RedisKeys.postReplies(postId), 0, limit - 1);
        if (members == null) {
            return List.of();
        }
        return members.stream().map(Long::parseLong).toList();
    }
}
