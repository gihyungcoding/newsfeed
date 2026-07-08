package com.newsfeed.engagement.domain.event;

public record PostUnlikedEvent(long postId, long userId) {
}
