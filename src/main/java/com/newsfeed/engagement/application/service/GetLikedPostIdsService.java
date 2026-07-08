package com.newsfeed.engagement.application.service;

import com.newsfeed.engagement.application.port.in.GetLikedPostIdsUseCase;
import com.newsfeed.engagement.application.port.out.LikeRepositoryPort;
import com.newsfeed.engagement.application.port.out.PostLikersCachePort;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GetLikedPostIdsService implements GetLikedPostIdsUseCase {

    private final PostLikersCachePort postLikersCache;
    private final LikeRepositoryPort likeRepository;

    public GetLikedPostIdsService(PostLikersCachePort postLikersCache, LikeRepositoryPort likeRepository) {
        this.postLikersCache = postLikersCache;
        this.likeRepository = likeRepository;
    }

    @Override
    public Set<Long> likedPostIds(List<Long> postIds, long userId) {
        Set<Long> liked = new HashSet<>();
        for (Long postId : postIds) {
            boolean isLiked = postLikersCache.exists(postId)
                    ? postLikersCache.isMember(postId, userId)
                    : likeRepository.exists(postId, userId); // 캐시 콜드 상태 — DB로 판정 (캐시는 되채우지 않음)
            if (isLiked) {
                liked.add(postId);
            }
        }
        return liked;
    }
}
