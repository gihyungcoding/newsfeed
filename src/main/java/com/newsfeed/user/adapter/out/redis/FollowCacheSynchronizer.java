package com.newsfeed.user.adapter.out.redis;

import com.newsfeed.user.application.port.out.FollowingCachePort;
import com.newsfeed.user.application.port.out.UserCountCachePort;
import com.newsfeed.user.domain.event.UserFollowedEvent;
import com.newsfeed.user.domain.event.UserUnfollowedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 팔로우 트랜잭션이 "커밋된 후"에만 캐시를 갱신한다 (AFTER_COMMIT이 기본 phase).
 * 롤백된 변경이 캐시에 반영되는 것을 막는다. 캐시 갱신이 실패해도
 * look-aside 재적재로 복구되므로 여기서 실패를 전파하지 않아도 된다.
 */
@Component
class FollowCacheSynchronizer {

    private final UserCountCachePort countCache;
    private final FollowingCachePort followingCache;

    FollowCacheSynchronizer(UserCountCachePort countCache, FollowingCachePort followingCache) {
        this.countCache = countCache;
        this.followingCache = followingCache;
    }

    @TransactionalEventListener
    void on(UserFollowedEvent event) {
        countCache.incrementIfPresent(event.followeeId(), 1, 0);   // 대상: 팔로워 +1
        countCache.incrementIfPresent(event.followerId(), 0, 1);   // 나: 팔로잉 +1
        followingCache.evict(event.followerId());                  // 내 팔로잉 목록 무효화
    }

    @TransactionalEventListener
    void on(UserUnfollowedEvent event) {
        countCache.incrementIfPresent(event.followeeId(), -1, 0);
        countCache.incrementIfPresent(event.followerId(), 0, -1);
        followingCache.evict(event.followerId());
    }
}
