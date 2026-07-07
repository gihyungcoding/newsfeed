package com.newsfeed.feed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * newsfeed.fanout.celebrity-threshold — fanout(쓰기 경로)과 feed(읽기 경로)가 반드시 같은 값을
 * 봐야 하는 하이브리드 팬아웃의 정책값이다. fanout의 {@code FanoutProperties}와 같은 프로퍼티
 * 키에 바인딩하는 별도의 작은 레코드를 둔다 — fanout의 {@code FeedCacheTtlProperties}가
 * "newsfeed.feed" 네임스페이스에 바인딩하는 것과 같은 전례를 따른다.
 */
@ConfigurationProperties(prefix = "newsfeed.fanout")
public record CelebrityThresholdProperties(int celebrityThreshold) {
}
