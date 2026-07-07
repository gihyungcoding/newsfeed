package com.newsfeed.user.application.service;

import com.newsfeed.common.error.ApiException;
import com.newsfeed.user.application.port.out.FollowRepositoryPort;
import com.newsfeed.user.application.port.out.UserEventPublisherPort;
import com.newsfeed.user.application.port.out.UserRepositoryPort;
import com.newsfeed.user.domain.event.UserFollowedEvent;
import com.newsfeed.user.domain.event.UserUnfollowedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 애플리케이션 계층 단위 테스트 — Spring 컨테이너 없이 포트를 모킹해서 검증한다.
 * 클린 아키텍처의 이점: 비즈니스 규칙을 DB/Redis 없이 빠르게 테스트할 수 있다.
 */
@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    UserRepositoryPort userRepository;

    @Mock
    FollowRepositoryPort followRepository;

    @Mock
    UserEventPublisherPort eventPublisher;

    @InjectMocks
    FollowService followService;

    @Test
    void 팔로우하면_관계_저장과_횟수_증가와_이벤트_발행이_일어난다() {
        given(userRepository.existsById(1L)).willReturn(true);
        given(userRepository.existsById(2L)).willReturn(true);
        given(followRepository.exists(1L, 2L)).willReturn(false);

        followService.follow(1L, 2L);

        verify(followRepository).insert(1L, 2L);
        verify(userRepository).incrementFollowerCount(2L, 1);   // 대상의 팔로워 +1
        verify(userRepository).incrementFollowingCount(1L, 1);  // 나의 팔로잉 +1
        verify(eventPublisher).publish(new UserFollowedEvent(1L, 2L));
    }

    @Test
    void 자기_자신은_팔로우할_수_없다() {
        assertThatThrownBy(() -> followService.follow(1L, 1L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CANNOT_FOLLOW_SELF"));
    }

    @Test
    void 이미_팔로우_중이면_충돌_오류가_난다() {
        given(userRepository.existsById(1L)).willReturn(true);
        given(userRepository.existsById(2L)).willReturn(true);
        given(followRepository.exists(1L, 2L)).willReturn(true);

        assertThatThrownBy(() -> followService.follow(1L, 2L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("ALREADY_FOLLOWING"));

        verify(followRepository, never()).insert(1L, 2L);
    }

    @Test
    void 존재하지_않는_사용자를_팔로우하면_404다() {
        given(userRepository.existsById(1L)).willReturn(true);
        given(userRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> followService.follow(1L, 99L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("USER_NOT_FOUND"));
    }

    @Test
    void 언팔로우하면_횟수가_감소하고_이벤트가_발행된다() {
        given(followRepository.delete(1L, 2L)).willReturn(true);

        followService.unfollow(1L, 2L);

        verify(userRepository).incrementFollowerCount(2L, -1);
        verify(userRepository).incrementFollowingCount(1L, -1);
        verify(eventPublisher).publish(new UserUnfollowedEvent(1L, 2L));
    }

    @Test
    void 팔로우_상태가_아니면_언팔로우는_아무_일도_하지_않는다() {
        given(followRepository.delete(1L, 2L)).willReturn(false);

        followService.unfollow(1L, 2L);

        verify(userRepository, never()).incrementFollowerCount(2L, -1);
        verify(eventPublisher, never()).publish(new UserUnfollowedEvent(1L, 2L));
    }
}
