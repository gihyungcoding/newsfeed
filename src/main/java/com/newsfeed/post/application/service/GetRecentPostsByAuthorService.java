package com.newsfeed.post.application.service;

import com.newsfeed.post.application.port.in.GetPostsByIdsUseCase;
import com.newsfeed.post.application.port.in.GetRecentPostsByAuthorUseCase;
import com.newsfeed.post.application.port.out.AuthorPostsCachePort;
import com.newsfeed.post.application.port.out.PostRepositoryPort;
import com.newsfeed.post.domain.Post;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetRecentPostsByAuthorService implements GetRecentPostsByAuthorUseCase {

    private final AuthorPostsCachePort authorPostsCache;
    private final PostRepositoryPort postRepository;
    private final GetPostsByIdsUseCase getPostsByIdsUseCase;

    public GetRecentPostsByAuthorService(AuthorPostsCachePort authorPostsCache,
                                         PostRepositoryPort postRepository,
                                         GetPostsByIdsUseCase getPostsByIdsUseCase) {
        this.authorPostsCache = authorPostsCache;
        this.postRepository = postRepository;
        this.getPostsByIdsUseCase = getPostsByIdsUseCase;
    }

    @Override
    public List<GetPostsByIdsUseCase.PostView> recent(long authorId, int limit) {
        List<Long> cachedIds = authorPostsCache.recentPostIds(authorId, limit);
        if (!cachedIds.isEmpty()) {
            return getPostsByIdsUseCase.list(cachedIds);
        }
        // 콜드 스타트: author-posts 캐시가 비어 있으면(TTL 만료 등) DB에서 재구성하고 캐시를 다시 채운다
        List<Post> fromDb = postRepository.findByAuthor(authorId, null, limit);
        fromDb.forEach(post -> authorPostsCache.push(authorId, post.id(), post.createdAt().toEpochMilli()));
        return fromDb.stream().map(GetPostsByIdsUseCase.PostView::of).toList();
    }
}
