package com.newsfeed.post.application.port.out;

import com.newsfeed.post.domain.Post;
import com.newsfeed.post.domain.PostCounts;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PostRepositoryPort {

    Post save(Post post);

    Optional<Post> findById(long postId);

    /** authorId의 포스트를 작성시각 역순으로, cursor(있으면 그 시각보다 이전 것만) 페이지 조회. */
    List<Post> findByAuthor(long authorId, Instant cursor, int size);

    /** 캐시 miss로 남은 ID들만 한 번의 IN 쿼리로 조회한다 (건별 findById 반복을 피한다). */
    List<Post> findAllByIds(List<Long> postIds);

    Optional<PostCounts> findCounts(long postId);

    /** posts.like_count를 원자적으로 증감한다 (UPDATE ... SET x = x + delta). */
    void incrementLikeCount(long postId, int delta);

    void incrementReplyCount(long postId, int delta);
}
