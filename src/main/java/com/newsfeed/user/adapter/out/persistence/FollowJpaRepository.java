package com.newsfeed.user.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface FollowJpaRepository extends JpaRepository<FollowJpaEntity, FollowJpaEntity.FollowId> {

    boolean existsById_FollowerIdAndId_FolloweeId(long followerId, long followeeId);

    long deleteById_FollowerIdAndId_FolloweeId(long followerId, long followeeId);

    // idx_follows_followee(followee_id, follower_id) 인덱스를 그대로 타는 범위 조회
    @Query("select f.id.followerId from FollowJpaEntity f "
            + "where f.id.followeeId = :followeeId and (:afterFollowerId is null or f.id.followerId > :afterFollowerId) "
            + "order by f.id.followerId asc")
    List<Long> findFollowerIds(@Param("followeeId") long followeeId,
                               @Param("afterFollowerId") Long afterFollowerId,
                               Pageable pageable);
}
