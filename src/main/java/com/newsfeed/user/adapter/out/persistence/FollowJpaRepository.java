package com.newsfeed.user.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface FollowJpaRepository extends JpaRepository<FollowJpaEntity, FollowJpaEntity.FollowId> {

    boolean existsById_FollowerIdAndId_FolloweeId(long followerId, long followeeId);

    long deleteById_FollowerIdAndId_FolloweeId(long followerId, long followeeId);
}
