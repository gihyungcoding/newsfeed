package com.newsfeed.post.domain.event;

import java.time.Instant;

/**
 * 포스트 발행 도메인 이벤트. 트랜잭션 커밋 후 두 구독자가 반응한다:
 * 콘텐츠 캐시 적재(같은 컨텍스트)와 Kafka post-created 발행(팬아웃 트리거).
 */
public record PostCreatedEvent(long postId, long authorId, String content, Instant createdAt) {
}
