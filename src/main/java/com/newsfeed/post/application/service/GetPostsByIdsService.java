package com.newsfeed.post.application.service;

import com.newsfeed.post.application.port.in.GetPostsByIdsUseCase;
import com.newsfeed.post.application.port.out.PostCachePort;
import com.newsfeed.post.application.port.out.PostCountCachePort;
import com.newsfeed.post.application.port.out.PostRepositoryPort;
import com.newsfeed.post.domain.Post;
import com.newsfeed.post.domain.PostCounts;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GetPostsByIdsService implements GetPostsByIdsUseCase {

    private final PostCachePort postCache;
    private final PostRepositoryPort postRepository;
    private final PostCountCachePort postCountCache;

    public GetPostsByIdsService(PostCachePort postCache, PostRepositoryPort postRepository,
                                PostCountCachePort postCountCache) {
        this.postCache = postCache;
        this.postRepository = postRepository;
        this.postCountCache = postCountCache;
    }

    @Override
    public List<PostView> list(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Post> posts = resolvePosts(postIds);
        Map<Long, PostCounts> counts = resolveCounts(postIds);

        return postIds.stream()
                .filter(posts::containsKey)
                .map(id -> PostView.of(posts.get(id), counts.getOrDefault(id, PostCounts.zero())))
                .toList();
    }

    private Map<Long, Post> resolvePosts(List<Long> postIds) {
        Map<Long, Post> resolved = new HashMap<>();
        List<Long> missIds = new ArrayList<>();
        for (Long id : postIds) {
            postCache.find(id).ifPresentOrElse(post -> resolved.put(id, post), () -> missIds.add(id));
        }
        if (!missIds.isEmpty()) {
            // 캐시 miss만 모아 한 번의 IN 쿼리로 조회 — 건별 findById 반복(N+1)을 피한다
            for (Post post : postRepository.findAllByIds(missIds)) {
                resolved.put(post.id(), post);
                postCache.save(post);
            }
        }
        return resolved;
    }

    private Map<Long, PostCounts> resolveCounts(List<Long> postIds) {
        Map<Long, PostCounts> resolved = new HashMap<>();
        for (Long id : postIds) {
            PostCounts counts = postCountCache.find(id).orElseGet(() -> {
                PostCounts loaded = postRepository.findCounts(id).orElse(PostCounts.zero());
                postCountCache.save(id, loaded);
                return loaded;
            });
            resolved.put(id, counts);
        }
        return resolved;
    }
}
