package com.newsfeed.post.application.port.out;

import java.util.List;

/**
 * 작성자별 최근 포스트 캐시 (author-posts:{authorId}, Sorted Set). 책의 5계층 중 "인기 콘텐츠".
 * 발행 시 write-through로 적재해, celebrity의 포스트를 조회 시점에 pull하는 경로(fanout-on-read)가
 * 매번 DB를 치지 않게 한다 (docs/03-detailed-design.md §3.2, §3.4).
 */
public interface AuthorPostsCachePort {

    void push(long authorId, long postId, long createdAtEpochMillis);

    /** 최근 postId 최대 limit개. 캐시가 비어 있으면(콜드 스타트) 빈 목록을 반환한다 — 호출자가 DB로 fallback한다. */
    List<Long> recentPostIds(long authorId, int limit);
}
