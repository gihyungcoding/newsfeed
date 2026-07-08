package com.newsfeed.engagement.application.service;

import com.newsfeed.common.error.ApiException;
import com.newsfeed.engagement.application.port.in.LikePostUseCase;
import com.newsfeed.engagement.application.port.out.EngagementEventPublisherPort;
import com.newsfeed.engagement.application.port.out.LikeRepositoryPort;
import com.newsfeed.engagement.domain.event.PostLikedEvent;
import com.newsfeed.engagement.domain.event.PostUnlikedEvent;
import com.newsfeed.post.application.port.in.GetPostsByIdsUseCase;
import com.newsfeed.post.application.port.in.UpdatePostCountsUseCase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 좋아요/취소. FollowService(user 컨텍스트)와 같은 구조 — DB 쓰기(관계 + 횟수 증감)는
 * 한 트랜잭션으로 묶고, Redis 캐시 동기화는 도메인 이벤트를 통해 커밋 후 처리한다.
 *
 * <p>횟수 증감은 이 트랜잭션 안에서 post 컨텍스트의 {@code UpdatePostCountsUseCase}를 호출해
 * 처리한다 — engagement는 posts 테이블이나 cnt:post 캐시를 직접 건드리지 않는다.
 */
@Service
public class LikePostService implements LikePostUseCase {

    private final LikeRepositoryPort likeRepository;
    private final GetPostsByIdsUseCase getPostsByIdsUseCase;
    private final UpdatePostCountsUseCase updatePostCountsUseCase;
    private final EngagementEventPublisherPort eventPublisher;

    public LikePostService(LikeRepositoryPort likeRepository,
                           GetPostsByIdsUseCase getPostsByIdsUseCase,
                           UpdatePostCountsUseCase updatePostCountsUseCase,
                           EngagementEventPublisherPort eventPublisher) {
        this.likeRepository = likeRepository;
        this.getPostsByIdsUseCase = getPostsByIdsUseCase;
        this.updatePostCountsUseCase = updatePostCountsUseCase;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void like(long postId, long userId) {
        requirePostExists(postId);
        if (likeRepository.exists(postId, userId)) {
            return; // 이미 좋아요 상태 — 멱등
        }
        try {
            likeRepository.insert(postId, userId);
        } catch (DataIntegrityViolationException e) {
            // exists 확인과 insert 사이의 동시 중복 좋아요 경합 — 복합 PK가 최종 방어, 멱등하게 흡수
            return;
        }
        updatePostCountsUseCase.incrementLikeCount(postId, 1);
        eventPublisher.publish(new PostLikedEvent(postId, userId));
    }

    @Override
    @Transactional
    public void unlike(long postId, long userId) {
        if (!likeRepository.delete(postId, userId)) {
            return; // 좋아요 상태가 아니었음 — 멱등
        }
        updatePostCountsUseCase.incrementLikeCount(postId, -1);
        eventPublisher.publish(new PostUnlikedEvent(postId, userId));
    }

    private void requirePostExists(long postId) {
        if (getPostsByIdsUseCase.list(List.of(postId)).isEmpty()) {
            throw ApiException.notFound("POST_NOT_FOUND", "포스트를 찾을 수 없습니다: " + postId);
        }
    }
}
