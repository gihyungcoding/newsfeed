package com.newsfeed.engagement.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface LikeJpaRepository extends JpaRepository<LikeJpaEntity, LikeJpaEntity.LikeId> {

    boolean existsById_PostIdAndId_UserId(long postId, long userId);

    long deleteById_PostIdAndId_UserId(long postId, long userId);
}
