package com.newsfeed.post.domain.event;

/**
 * 좋아요/답글 횟수가 바뀐 뒤 발행되는 이벤트. engagement 컨텍스트가 이 이벤트를 직접 발행하지
 * 않는다 — post 컨텍스트의 {@code UpdatePostCountsUseCase}를 호출하면 post 컨텍스트 스스로가
 * DB 증분과 함께 이 이벤트를 발행하고, post 자신의 리스너가 Redis 캐시를 갱신한다.
 * "post의 횟수"는 끝까지 post 컨텍스트가 소유한다는 원칙을 지키기 위함이다.
 */
public record PostCountsChangedEvent(long postId, int likeDelta, int replyDelta) {
}
