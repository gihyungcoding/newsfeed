package com.newsfeed.engagement.adapter.out.redis;

import com.newsfeed.engagement.application.port.out.PostLikersCachePort;
import com.newsfeed.engagement.application.port.out.PostRepliesCachePort;
import com.newsfeed.engagement.domain.event.PostLikedEvent;
import com.newsfeed.engagement.domain.event.PostUnlikedEvent;
import com.newsfeed.engagement.domain.event.ReplyCreatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 좋아요/답글 트랜잭션이 커밋된 후에만 행동 캐시를 갱신한다 (user 컨텍스트의
 * FollowCacheSynchronizer, post 컨텍스트의 PostCacheSynchronizer와 같은 이유).
 */
@Component
class EngagementCacheSynchronizer {

    private final PostLikersCachePort postLikersCache;
    private final PostRepliesCachePort postRepliesCache;

    EngagementCacheSynchronizer(PostLikersCachePort postLikersCache, PostRepliesCachePort postRepliesCache) {
        this.postLikersCache = postLikersCache;
        this.postRepliesCache = postRepliesCache;
    }

    @TransactionalEventListener
    void on(PostLikedEvent event) {
        postLikersCache.add(event.postId(), event.userId());
    }

    @TransactionalEventListener
    void on(PostUnlikedEvent event) {
        postLikersCache.remove(event.postId(), event.userId());
    }

    @TransactionalEventListener
    void on(ReplyCreatedEvent event) {
        postRepliesCache.push(event.postId(), event.replyId(), event.createdAt().toEpochMilli());
    }
}
