package com.newsfeed.post.application.port.out;

import com.newsfeed.post.domain.Post;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PostRepositoryPort {

    Post save(Post post);

    Optional<Post> findById(long postId);

    /** authorId의 포스트를 작성시각 역순으로, cursor(있으면 그 시각보다 이전 것만) 페이지 조회. */
    List<Post> findByAuthor(long authorId, Instant cursor, int size);
}
