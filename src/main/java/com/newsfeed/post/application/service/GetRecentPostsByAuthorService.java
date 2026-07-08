package com.newsfeed.post.application.service;

import com.newsfeed.post.application.port.in.GetPostsByIdsUseCase;
import com.newsfeed.post.application.port.in.GetRecentPostsByAuthorUseCase;
import com.newsfeed.post.application.port.out.AuthorPostsCachePort;
import com.newsfeed.post.application.port.out.PostCachePort;
import com.newsfeed.post.application.port.out.PostRepositoryPort;
import com.newsfeed.post.domain.Post;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetRecentPostsByAuthorService implements GetRecentPostsByAuthorUseCase {

    private final AuthorPostsCachePort authorPostsCache;
    private final PostCachePort postCache;
    private final PostRepositoryPort postRepository;
    private final GetPostsByIdsUseCase getPostsByIdsUseCase;

    public GetRecentPostsByAuthorService(AuthorPostsCachePort authorPostsCache,
                                         PostCachePort postCache,
                                         PostRepositoryPort postRepository,
                                         GetPostsByIdsUseCase getPostsByIdsUseCase) {
        this.authorPostsCache = authorPostsCache;
        this.postCache = postCache;
        this.postRepository = postRepository;
        this.getPostsByIdsUseCase = getPostsByIdsUseCase;
    }

    @Override
    public List<GetPostsByIdsUseCase.PostView> recent(long authorId, int limit) {
        List<Long> cachedIds = authorPostsCache.recentPostIds(authorId, limit);
        if (!cachedIds.isEmpty()) {
            return getPostsByIdsUseCase.list(cachedIds);
        }
        // 콜드 스타트: author-posts 캐시가 비어 있으면(TTL 만료 등) DB에서 재구성한다.
        // 콘텐츠 조립(+횟수 조립)은 getPostsByIdsUseCase에 위임한다 — 여기서 직접 PostView를
        // 만들면 횟수 조립 로직이 두 곳에 중복된다. 그러기 위해 post 캐시부터 미리 채워
        // list() 호출이 방금 조회한 내용을 DB에서 또 읽지 않게 한다.
        List<Post> fromDb = postRepository.findByAuthor(authorId, null, limit);
        fromDb.forEach(post -> {
            authorPostsCache.push(authorId, post.id(), post.createdAt().toEpochMilli());
            postCache.save(post);
        });
        return getPostsByIdsUseCase.list(fromDb.stream().map(Post::id).toList());
    }
}
