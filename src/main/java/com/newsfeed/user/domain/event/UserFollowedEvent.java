package com.newsfeed.user.domain.event;

/** 팔로우 성립 도메인 이벤트. 트랜잭션 커밋 후 캐시 동기화에 사용된다. */
public record UserFollowedEvent(long followerId, long followeeId) {
}
