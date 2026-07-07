package com.newsfeed.fanout.application.port.in;

import java.time.Instant;

/**
 * post-created 이벤트를 받아 팔로워들의 피드 캐시에 전파하는 유스케이스.
 * Kafka 컨슈머 어댑터가 이 인터페이스만 호출하도록 분리해, 실제 Kafka 없이도
 * 팬아웃 로직(하이브리드 임계값 판정 등)을 단위 테스트할 수 있게 한다.
 */
public interface ProcessPostCreatedUseCase {

    void handle(long postId, long authorId, Instant createdAt);
}
