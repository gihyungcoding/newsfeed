package com.newsfeed.post.adapter.out.redis;

import com.newsfeed.post.application.port.out.PostCountCachePort;
import com.newsfeed.post.domain.event.PostCountsChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * UpdatePostCountsService가 DB 증분 후 발행한 이벤트를 커밋 후에 Redis에 반영한다.
 * 이 이벤트는 engagement 컨텍스트의 좋아요/답글 트랜잭션 안에서 호출되지만, Spring의
 * 트랜잭션 동기화는 스레드에 바인딩된 "현재 트랜잭션" 기준이라 호출 경로(어느 컨텍스트가
 * 호출했는지)와 무관하게 그 트랜잭션이 최종 커밋된 후에만 이 리스너가 실행된다.
 */
@Component
class PostCountsCacheSynchronizer {

    private final PostCountCachePort postCountCache;

    PostCountsCacheSynchronizer(PostCountCachePort postCountCache) {
        this.postCountCache = postCountCache;
    }

    @TransactionalEventListener
    void on(PostCountsChangedEvent event) {
        postCountCache.incrementIfPresent(event.postId(), event.likeDelta(), event.replyDelta());
    }
}
