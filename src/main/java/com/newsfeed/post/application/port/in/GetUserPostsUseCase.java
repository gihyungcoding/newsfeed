package com.newsfeed.post.application.port.in;

import com.newsfeed.post.domain.Post;

import java.util.List;

/** 특정 작성자의 포스트를 커서 기반으로 조회한다. 콜드 스타트 시 celebrity pull에도 재사용된다. */
public interface GetUserPostsUseCase {

    PagedPosts list(long authorId, Long cursorEpochMillis, int size);

    record PagedPosts(List<Post> items, Long nextCursor) {
    }
}
