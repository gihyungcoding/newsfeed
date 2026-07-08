package com.newsfeed.engagement.domain.event;

import java.time.Instant;

public record ReplyCreatedEvent(long replyId, long postId, long authorId, Instant createdAt) {
}
