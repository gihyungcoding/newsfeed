package com.newsfeed.user.application.port.out;

import java.util.List;

/** 팔로우 관계 영속성 아웃바운드 포트. */
public interface FollowRepositoryPort {

    boolean exists(long followerId, long followeeId);

    void insert(long followerId, long followeeId);

    /** @return 실제로 삭제됐으면 true (멱등 언팔로우 판정에 사용) */
    boolean delete(long followerId, long followeeId);

    /** followerId 오름차순 페이지 조회. idx_follows_followee(followee_id, follower_id) 인덱스를 탄다. */
    List<Long> findFollowerIds(long followeeId, Long afterFollowerId, int limit);
}
