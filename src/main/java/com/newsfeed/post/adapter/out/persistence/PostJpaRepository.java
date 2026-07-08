package com.newsfeed.post.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

interface PostJpaRepository extends JpaRepository<PostJpaEntity, Long> {

    // idx_posts_author_created(author_id, created_at DESC) 인덱스를 그대로 탄다
    @Query("select p from PostJpaEntity p where p.authorId = :authorId "
            + "and (:cursor is null or p.createdAt < :cursor) order by p.createdAt desc")
    List<PostJpaEntity> findByAuthor(@Param("authorId") long authorId,
                                     @Param("cursor") Instant cursor,
                                     Pageable pageable);

    @Modifying
    @Query("update PostJpaEntity p set p.likeCount = p.likeCount + :delta where p.id = :postId")
    int incrementLikeCount(@Param("postId") long postId, @Param("delta") int delta);

    @Modifying
    @Query("update PostJpaEntity p set p.replyCount = p.replyCount + :delta where p.id = :postId")
    int incrementReplyCount(@Param("postId") long postId, @Param("delta") int delta);
}
