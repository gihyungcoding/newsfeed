package com.newsfeed.fanout;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * newsfeed.fanout.* 설정 (docs/03-detailed-design.md §3.1, §3.3).
 *
 * @param celebrityThreshold 팔로워 수가 이 값을 넘으면 팬아웃을 생략한다 (하이브리드 전략)
 * @param followerBatchSize  팔로워 목록을 페이지 단위로 조회할 때의 페이지 크기
 * @param feedMaxSize        feed:{userId} Sorted Set의 상한
 */
@ConfigurationProperties(prefix = "newsfeed.fanout")
public record FanoutProperties(int celebrityThreshold, int followerBatchSize, int feedMaxSize) {
}
