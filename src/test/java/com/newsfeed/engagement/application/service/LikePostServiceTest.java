package com.newsfeed.engagement.application.service;

import com.newsfeed.common.error.ApiException;
import com.newsfeed.engagement.application.port.out.EngagementEventPublisherPort;
import com.newsfeed.engagement.application.port.out.LikeRepositoryPort;
import com.newsfeed.engagement.domain.event.PostLikedEvent;
import com.newsfeed.engagement.domain.event.PostUnlikedEvent;
import com.newsfeed.post.application.port.in.GetPostsByIdsUseCase;
import com.newsfeed.post.application.port.in.GetPostsByIdsUseCase.PostView;
import com.newsfeed.post.application.port.in.UpdatePostCountsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LikePostServiceTest {

    @Mock
    LikeRepositoryPort likeRepository;
    @Mock
    GetPostsByIdsUseCase getPostsByIdsUseCase;
    @Mock
    UpdatePostCountsUseCase updatePostCountsUseCase;
    @Mock
    EngagementEventPublisherPort eventPublisher;

    @InjectMocks
    LikePostService likePostService;

    private PostView existingPost() {
        return new PostView(100L, 1L, "content", 0, 0, Instant.now());
    }

    @Test
    void 좋아요하면_저장과_횟수_증가와_이벤트_발행이_일어난다() {
        given(getPostsByIdsUseCase.list(List.of(100L))).willReturn(List.of(existingPost()));
        given(likeRepository.exists(100L, 2L)).willReturn(false);

        likePostService.like(100L, 2L);

        verify(likeRepository).insert(100L, 2L);
        verify(updatePostCountsUseCase).incrementLikeCount(100L, 1);
        verify(eventPublisher).publish(new PostLikedEvent(100L, 2L));
    }

    @Test
    void 존재하지_않는_포스트에_좋아요하면_404다() {
        given(getPostsByIdsUseCase.list(List.of(100L))).willReturn(List.of());

        assertThatThrownBy(() -> likePostService.like(100L, 2L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("POST_NOT_FOUND"));

        verify(likeRepository, never()).insert(100L, 2L);
    }

    @Test
    void 이미_좋아요_상태면_멱등하게_통과한다() {
        given(getPostsByIdsUseCase.list(List.of(100L))).willReturn(List.of(existingPost()));
        given(likeRepository.exists(100L, 2L)).willReturn(true);

        likePostService.like(100L, 2L);

        verify(likeRepository, never()).insert(100L, 2L);
        verify(updatePostCountsUseCase, never()).incrementLikeCount(100L, 1);
    }

    @Test
    void 동시_중복_좋아요_경합은_조용히_흡수한다() {
        given(getPostsByIdsUseCase.list(List.of(100L))).willReturn(List.of(existingPost()));
        given(likeRepository.exists(100L, 2L)).willReturn(false);
        willThrow(new DataIntegrityViolationException("duplicate key")).given(likeRepository).insert(100L, 2L);

        likePostService.like(100L, 2L); // 예외 없이 조용히 종료

        verify(updatePostCountsUseCase, never()).incrementLikeCount(100L, 1);
    }

    @Test
    void 좋아요_취소하면_횟수가_감소하고_이벤트가_발행된다() {
        given(likeRepository.delete(100L, 2L)).willReturn(true);

        likePostService.unlike(100L, 2L);

        verify(updatePostCountsUseCase).incrementLikeCount(100L, -1);
        verify(eventPublisher).publish(new PostUnlikedEvent(100L, 2L));
    }

    @Test
    void 좋아요_상태가_아니면_취소는_아무_일도_하지_않는다() {
        given(likeRepository.delete(100L, 2L)).willReturn(false);

        likePostService.unlike(100L, 2L);

        verify(updatePostCountsUseCase, never()).incrementLikeCount(100L, -1);
        verify(eventPublisher, never()).publish(new PostUnlikedEvent(100L, 2L));
    }
}
