package com.newsfeed.user.domain.event;

/** 언팔로우 도메인 이벤트. */
public record UserUnfollowedEvent(long followerId, long followeeId) {
}
