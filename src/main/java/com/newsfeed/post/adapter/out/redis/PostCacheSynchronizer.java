package com.newsfeed.post.adapter.out.redis;

import com.newsfeed.post.application.port.out.AuthorPostsCachePort;
import com.newsfeed.post.application.port.out.PostCachePort;
import com.newsfeed.post.domain.Post;
import com.newsfeed.post.domain.event.PostCreatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 포스트 발행 트랜잭션이 커밋된 후에만 캐시를 적재한다 (user 컨텍스트의
 * FollowCacheSynchronizer와 같은 이유 — 롤백된 포스트가 캐시에 남지 않게 하기 위함).
 */
@Component
class PostCacheSynchronizer {

    private final PostCachePort postCache;
    private final AuthorPostsCachePort authorPostsCache;

    PostCacheSynchronizer(PostCachePort postCache, AuthorPostsCachePort authorPostsCache) {
        this.postCache = postCache;
        this.authorPostsCache = authorPostsCache;
    }

    @TransactionalEventListener
    void on(PostCreatedEvent event) {
        postCache.save(new Post(event.postId(), event.authorId(), event.content(), 0, 0, event.createdAt()));
        authorPostsCache.push(event.authorId(), event.postId(), event.createdAt().toEpochMilli());
    }
}
