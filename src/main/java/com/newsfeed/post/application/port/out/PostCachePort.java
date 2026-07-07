package com.newsfeed.post.application.port.out;

import com.newsfeed.post.domain.Post;

import java.util.Optional;

/** 포스트 콘텐츠 캐시 (post:{id}, look-aside). */
public interface PostCachePort {

    Optional<Post> find(long postId);

    void save(Post post);
}
