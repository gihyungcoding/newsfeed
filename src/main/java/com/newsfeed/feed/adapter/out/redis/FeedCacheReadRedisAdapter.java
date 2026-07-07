package com.newsfeed.feed.adapter.out.redis;

import com.newsfeed.common.cache.RedisKeys;
import com.newsfeed.feed.application.port.out.FeedCacheReadPort;
import com.newsfeed.feed.domain.PushedPost;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * feed:{userId} 캐시 읽기. Boot 4의 {@code ZSetOperations}는 (역)범위 조회에 {@code Range}/
 * {@code Limit} 조합이 아니라 {@code double} min/max 오버로드만 제공한다. score가 항상 정수
 * 밀리초이므로, "cursor 미만(제외)"을 표현하려고 작은 값을 빼는 방식(epsilon trick)을 쓴다 —
 * Redis CLI의 {@code ZREVRANGEBYSCORE key (cursor +inf} 문법과 동등한 효과를 낸다.
 *
 * <p>epsilon 크기 선택에 주의가 필요하다: cursor는 epoch millis라 이미 13자리 정수다.
 * double의 유효자릿수는 약 15~17자리뿐이라, {@code 0.000001}처럼 너무 작은 값을 빼면
 * 반올림으로 원래 값과 완전히 같아져 버려(제외가 동작하지 않음) 실제로 겪은 버그다.
 * 두 score(정수 밀리초) 사이의 최소 간격은 1이므로, 그 절반인 {@code 0.5}를 빼면 항상
 * 정밀도 문제 없이 "바로 아래 정수보다는 크고 cursor보다는 작은" 값이 된다.
 */
@Component
class FeedCacheReadRedisAdapter implements FeedCacheReadPort {

    private static final double EPSILON = 0.5;

    private final StringRedisTemplate redisTemplate;

    FeedCacheReadRedisAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<PushedPost> readPushed(long userId, Long cursorEpochMillis, int size) {
        double max = cursorEpochMillis == null ? Double.POSITIVE_INFINITY : cursorEpochMillis - EPSILON;
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(RedisKeys.feed(userId), Double.NEGATIVE_INFINITY, max, 0, size);
        if (tuples == null) {
            return List.of();
        }
        // Set의 반환 순서에 기대지 않고 score 기준으로 명시적으로 정렬한다 (방어적 정확성)
        return tuples.stream()
                .sorted(Comparator.comparing(ZSetOperations.TypedTuple<String>::getScore).reversed())
                .map(t -> new PushedPost(Long.parseLong(t.getValue()), t.getScore().longValue()))
                .toList();
    }
}
