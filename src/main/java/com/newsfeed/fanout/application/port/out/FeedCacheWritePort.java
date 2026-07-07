package com.newsfeed.fanout.application.port.out;

import java.util.List;

/**
 * 뉴스피드 캐시(feed:{userId})에 대한 팬아웃 워커의 쓰기 전용 포트.
 * feed 컨텍스트(5단계)는 같은 Redis 키를 읽기 전용 포트로 별도로 정의한다 —
 * 컨텍스트마다 자신에게 필요한 연산만 보는 포트를 갖는 것이 인터페이스 분리 원칙이다.
 */
public interface FeedCacheWritePort {

    /** 배치 전체를 하나의 Redis 파이프라인으로 처리한다 (docs/03-detailed-design.md §3.3). */
    void pushToFeeds(List<Long> followerIds, long postId, long createdAtEpochMillis);
}
