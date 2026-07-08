package com.newsfeed.engagement.application.port.in;

import com.newsfeed.engagement.domain.Reply;

public interface CreateReplyUseCase {

    Reply create(Command command);

    record Command(long postId, long authorId, String content) {
    }
}
