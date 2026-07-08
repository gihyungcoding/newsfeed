package com.newsfeed.engagement.application.port.in;

import com.newsfeed.engagement.domain.Reply;

import java.util.List;

public interface GetRepliesUseCase {

    PagedReplies list(long postId, Long cursorEpochMillis, int size);

    record PagedReplies(List<Reply> items, Long nextCursor) {
    }
}
