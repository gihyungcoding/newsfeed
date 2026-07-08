package com.newsfeed.engagement.application.port.out;

import java.util.List;

/**
 * 답글 행동 캐시 (post-replies:{postId}, Redis Sorted Set). post 컨텍스트의
 * AuthorPostsCachePort와 같은 패턴 — "최근 N개"만 담당하고, 그보다 깊은 페이지네이션은
 * 호출자가 DB로 처리한다.
 */
public interface PostRepliesCachePort {

    void push(long postId, long replyId, long createdAtEpochMillis);

    /** 캐시가 비어 있으면(콜드 스타트) 빈 목록을 반환한다 — 호출자가 DB로 fallback한다. */
    List<Long> recentReplyIds(long postId, int limit);
}
