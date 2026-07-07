package com.newsfeed.post.adapter.out.kafka;

/**
 * post-created 토픽의 메시지 스키마 (프로듀서 측). docs/04-api-spec.md §4.5와 동일한 필드.
 * fanout 컨텍스트는 같은 모양의 별도 클래스를 갖는다 — 두 컨텍스트가 나중에 별도
 * 서비스로 분리됐을 때 서로의 Java 클래스가 아니라 이 JSON 스키마(계약)만 공유하게 하기 위함이다.
 */
record PostCreatedMessage(long postId, long authorId, long createdAt) {
}
