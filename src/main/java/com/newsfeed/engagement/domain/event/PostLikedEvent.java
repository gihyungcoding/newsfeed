package com.newsfeed.engagement.domain.event;

public record PostLikedEvent(long postId, long userId) {
}
