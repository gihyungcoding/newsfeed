package com.newsfeed.feed.domain;

/**
 * feed:{userId} 캐시에 팬아웃 워커가 push해 둔 원시 엔트리 (postId + 작성시각).
 * 아직 콘텐츠가 조립되지 않은 상태라는 걸 명시하기 위해 {@code PostView}와 구분한다.
 */
public record PushedPost(long postId, long createdAtEpochMillis) {
}
