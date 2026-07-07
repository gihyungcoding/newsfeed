package com.newsfeed.post.application.port.in;

import com.newsfeed.post.domain.Post;

public interface CreatePostUseCase {

    Post create(Command command);

    record Command(long authorId, String content) {
    }
}
