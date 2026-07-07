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

    // follows와 users를 조인하는 명시적 연관관계가 없으므로(엔티티를 단순하게 유지하려는 선택),
    // JPQL의 다중 루트(comma) 문법으로 두 엔티티를 직접 결합한다.
    @Query("select f.id.followeeId from FollowJpaEntity f, UserJpaEntity u "
            + "where u.id = f.id.followeeId and f.id.followerId = :followerId and u.followerCount > :threshold")
    List<Long> findCelebrityFolloweeIds(@Param("followerId") long followerId, @Param("threshold") int threshold);
}
