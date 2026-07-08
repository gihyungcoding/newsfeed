package com.newsfeed.engagement.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

interface ReplyJpaRepository extends JpaRepository<ReplyJpaEntity, Long> {

    // idx_replies_post_created(post_id, created_at DESC) 인덱스를 그대로 탄다
    @Query("select r from ReplyJpaEntity r where r.postId = :postId "
            + "and (:cursor is null or r.createdAt < :cursor) order by r.createdAt desc")
    List<ReplyJpaEntity> findByPost(@Param("postId") long postId,
                                    @Param("cursor") Instant cursor,
                                    Pageable pageable);
}
