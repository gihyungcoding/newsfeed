package com.newsfeed.post.application.service;

import com.newsfeed.post.application.port.in.GetPostsByIdsUseCase;
import com.newsfeed.post.application.port.out.PostCachePort;
import com.newsfeed.post.application.port.out.PostRepositoryPort;
import com.newsfeed.post.domain.Post;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class GetPostsByIdsService implements GetPostsByIdsUseCase {

    private final PostCachePort postCache;
    private final PostRepositoryPort postRepository;

    public GetPostsByIdsService(PostCachePort postCache, PostRepositoryPort postRepository) {
        this.postCache = postCache;
        this.postRepository = postRepository;
    }

    @Override
    public List<PostView> list(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Post> resolved = new LinkedHashMap<>();
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
        return postIds.stream().map(resolved::get).filter(Objects::nonNull).map(PostView::of).toList();
    }
}
