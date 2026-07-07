package com.newsfeed.fanout.adapter.out.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * newsfeed.feed.cache-ttl-days — 피드 캐시의 보관 기간 정책. "feed" 네임스페이스에 있는 이유는
 * 이 값이 fanout(쓰기)이 아니라 피드 캐시 자체의 보존 정책을 나타내기 때문이다.
 * 지금은 팬아웃 워커(쓰기 시점에 EXPIRE)만 이 값을 사용하고, feed 컨텍스트(5단계, 읽기 전용)는
 * TTL을 다루지 않으므로 별도로 필요하지 않다.
 */
@ConfigurationProperties(prefix = "newsfeed.feed")
record FeedCacheTtlProperties(int cacheTtlDays) {
}
