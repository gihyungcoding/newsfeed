package com.newsfeed.user.application.service;

import com.newsfeed.common.error.ApiException;
import com.newsfeed.user.application.port.in.FollowUseCase;
import com.newsfeed.user.application.port.out.FollowRepositoryPort;
import com.newsfeed.user.application.port.out.UserEventPublisherPort;
import com.newsfeed.user.application.port.out.UserRepositoryPort;
import com.newsfeed.user.domain.event.UserFollowedEvent;
import com.newsfeed.user.domain.event.UserUnfollowedEvent;
import org.springframework.dao.DataIntegrityViolationException;
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

        try {
            followRepository.insert(followerId, targetId);
        } catch (DataIntegrityViolationException e) {
            // exists 확인과 INSERT 사이에 동시 요청이 먼저 커밋한 경합 — 복합 PK가 최종 방어한다
            throw ApiException.conflict("ALREADY_FOLLOWING", "이미 팔로우 중입니다");
        }
        applyCountDeltas(followerId, targetId, 1);

        eventPublisher.publish(new UserFollowedEvent(followerId, targetId));
    }

    @Override
    @Transactional
    public void unfollow(long followerId, long targetId) {
        // 멱등: 팔로우 상태가 아니면 아무 일도 일어나지 않는다
        if (!followRepository.delete(followerId, targetId)) {
            return;
        }
        applyCountDeltas(followerId, targetId, -1);

        eventPublisher.publish(new UserUnfollowedEvent(followerId, targetId));
    }

    /**
     * 두 users 행의 잠금을 항상 id 오름차순으로 획득한다.
     * A→B 팔로우와 B→A 팔로우가 동시에 실행될 때 서로의 행을 반대 순서로 잠그면
     * 데드락이 발생할 수 있다 — 잠금 순서를 전역적으로 통일하면 원천 차단된다.
     */
    private void applyCountDeltas(long followerId, long targetId, int delta) {
        if (followerId < targetId) {
            userRepository.incrementFollowingCount(followerId, delta);
            userRepository.incrementFollowerCount(targetId, delta);
        } else {
            userRepository.incrementFollowerCount(targetId, delta);
            userRepository.incrementFollowingCount(followerId, delta);
        }
    }

    private void requireUser(long userId) {
        if (!userRepository.existsById(userId)) {
            throw ApiException.notFound("USER_NOT_FOUND", "사용자를 찾을 수 없습니다: " + userId);
        }
    }
}
