package com.newsfeed.engagement.application.port.in;

/** 좋아요/취소. 둘 다 멱등이다 — 이미 좋아요 상태에서 다시 좋아요해도, 아닌 상태에서 취소해도 조용히 성공한다. */
public interface LikePostUseCase {

    void like(long postId, long userId);

    void unlike(long postId, long userId);
}
