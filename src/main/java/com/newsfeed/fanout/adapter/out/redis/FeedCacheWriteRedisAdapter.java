package com.newsfeed.fanout.adapter.out.redis;

import com.newsfeed.common.cache.JitteredTtl;
import com.newsfeed.common.cache.RedisKeys;
import com.newsfeed.fanout.FanoutProperties;
import com.newsfeed.fanout.application.port.out.FeedCacheWritePort;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 팔로워 배치 전체를 Redis pipeline 하나로 처리한다 — 팔로워 1,000명 = 3,000개 명령
 * (ZADD + ZREMRANGEBYRANK + EXPIRE)을 왕복 1번으로 끝낸다 (docs/03-detailed-design.md §3.3).
 *
 * <p>파이프라이닝은 {@code RedisCallback}으로 저수준 커넥션을 직접 다루는 대신
 * {@code SessionCallback} + {@code RedisOperations}(템플릿 고수준 API)로 구현한다.
 * 저수준 방식은 콜백에 전달되는 커넥션을 {@code StringRedisConnection}으로 캐스팅해야 하는데,
 * Actuator의 옵저버빌리티 계측이 커넥션을 프록시로 감싸면 그 캐스팅이 런타임에 깨진다
 * (실제로 겪은 {@code ClassCastException}). 템플릿 API 경유는 그런 계측 유무와 무관하다.
 */
@Component
class FeedCacheWriteRedisAdapter implements FeedCacheWritePort {

    private final StringRedisTemplate redisTemplate;
    private final FanoutProperties fanoutProperties;
    private final FeedCacheTtlProperties ttlProperties;

    FeedCacheWriteRedisAdapter(StringRedisTemplate redisTemplate,
                              FanoutProperties fanoutProperties,
                              FeedCacheTtlProperties ttlProperties) {
        this.redisTemplate = redisTemplate;
        this.fanoutProperties = fanoutProperties;
        this.ttlProperties = ttlProperties;
    }

    @Override
    public void pushToFeeds(List<Long> followerIds, long postId, long createdAtEpochMillis) {
        int maxSize = fanoutProperties.feedMaxSize();
        Duration ttl = JitteredTtl.of(Duration.ofDays(ttlProperties.cacheTtlDays()), 0.1);
        String postIdValue = String.valueOf(postId);

        redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings("unchecked")
            public Object execute(RedisOperations operations) {
                for (long followerId : followerIds) {
                    String key = RedisKeys.feed(followerId);
                    operations.opsForZSet().add(key, postIdValue, createdAtEpochMillis);
                    operations.opsForZSet().removeRange(key, 0, -(maxSize + 1)); // 상한 유지
                    operations.expire(key, ttl);
                }
                return null;
            }
        });
    }
}
