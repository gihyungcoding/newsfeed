package com.newsfeed.feed.application.port.out;

import com.newsfeed.feed.domain.PushedPost;

import java.util.List;

/** feed:{userId} 캐시 읽기 전용 포트. fanout 컨텍스트의 쓰기 전용 포트와는 별개다 (§ISP). */
public interface FeedCacheReadPort {

    /** cursor(작성시각 millis) 이전 것만, 최신순 최대 size개. cursor가 null이면 처음부터. */
    List<PushedPost> readPushed(long userId, Long cursorEpochMillis, int size);
}
