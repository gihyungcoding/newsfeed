package com.newsfeed.user.application.service;

import com.newsfeed.common.error.ApiException;
import com.newsfeed.user.application.port.in.FollowUseCase;
import com.newsfeed.user.application.port.out.FollowRepositoryPort;
import com.newsfeed.user.application.port.out.UserEventPublisherPort;
import com.newsfeed.user.application.port.out.UserRepositoryPort;
import com.newsfeed.user.domain.event.UserFollowedEvent;
import com.newsfeed.user.domain.event.UserUnfollowedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팔로우/언팔로우 유스케이스.
 *
 * <p>트랜잭션 안에서는 DB(원천)만 다루고, Redis 캐시 갱신은 도메인 이벤트를 발행해
 * 커밋 후(AFTER_COMMIT)에 수행한다 — 롤백된 트랜잭션이 캐시를 오염시키지 않게 하기 위함이다.
 * (docs/03-detailed-design.md §3.5)
 */
@Service
public class FollowService implements FollowUseCase {

    private final UserRepositoryPort userRepository;
    private final FollowRepositoryPort followRepository;
    private final UserEventPublisherPort eventPublisher;

    public FollowService(UserRepositoryPort userRepository,
                         FollowRepositoryPort followRepository,
                         UserEventPublisherPort eventPublisher) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void follow(long followerId, long targetId) {
        if (followerId == targetId) {
            throw ApiException.badRequest("CANNOT_FOLLOW_SELF", "자기 자신을 팔로우할 수 없습니다");
        }
        requireUser(followerId);
        requireUser(targetId);
        if (followRepository.exists(followerId, targetId)) {
            throw ApiException.conflict("ALREADY_FOLLOWING", "이미 팔로우 중입니다");
        }

        followRepository.insert(followerId, targetId);
        userRepository.incrementFollowerCount(targetId, 1);
        userRepository.incrementFollowingCount(followerId, 1);

        eventPublisher.publish(new UserFollowedEvent(followerId, targetId));
    }

    @Override
    @Transactional
    public void unfollow(long followerId, long targetId) {
        // 멱등: 팔로우 상태가 아니면 아무 일도 일어나지 않는다
        if (!followRepository.delete(followerId, targetId)) {
            return;
        }
        userRepository.incrementFollowerCount(targetId, -1);
        userRepository.incrementFollowingCount(followerId, -1);

        eventPublisher.publish(new UserUnfollowedEvent(followerId, targetId));
    }

    private void requireUser(long userId) {
        if (!userRepository.existsById(userId)) {
            throw ApiException.notFound("USER_NOT_FOUND", "사용자를 찾을 수 없습니다: " + userId);
        }
    }
}
