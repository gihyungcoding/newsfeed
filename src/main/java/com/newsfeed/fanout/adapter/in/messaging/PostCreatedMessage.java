package com.newsfeed.fanout.adapter.in.messaging;

/**
 * post-created 메시지 스키마 (컨슈머 측). post 컨텍스트의 프로듀서 측 클래스와 필드는
 * 동일하지만 의도적으로 별개의 클래스다 — Kafka 토픽(문자열 이름)만이 두 컨텍스트의
 * 실제 계약이고, Java 클래스를 공유하지 않아야 나중에 별도 서비스로 분리하기 쉽다.
 */
record PostCreatedMessage(long postId, long authorId, long createdAt) {
}
