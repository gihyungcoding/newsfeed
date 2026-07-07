package com.newsfeed.user.application.port.out;

import com.newsfeed.user.domain.FollowCounts;

import java.util.Optional;

/**
 * 횟수 캐시 (cnt:user:{id}, Redis Hash). 책의 5계층 중 "횟수" 계층.
 * celebrity 판정(팔로워 수 임계값 비교)에도 사용된다.
 */
public interface UserCountCachePort {

    Optional<FollowCounts> find(long userId);

    void save(long userId, FollowCounts counts);

    /**
     * 캐시 키가 있을 때만 증분한다. 키가 없으면 아무것도 하지 않는다
     * — 다음 조회 miss 때 DB 컬럼값으로 재적재되므로 정합성이 복구된다.
     */
    void incrementIfPresent(long userId, int followerDelta, int followingDelta);
}
