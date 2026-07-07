package com.newsfeed.user.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    boolean existsByUsername(String username);

    @Modifying
    @Query("update UserJpaEntity u set u.followerCount = u.followerCount + :delta where u.id = :userId")
    int incrementFollowerCount(@Param("userId") long userId, @Param("delta") int delta);

    @Modifying
    @Query("update UserJpaEntity u set u.followingCount = u.followingCount + :delta where u.id = :userId")
    int incrementFollowingCount(@Param("userId") long userId, @Param("delta") int delta);
}
