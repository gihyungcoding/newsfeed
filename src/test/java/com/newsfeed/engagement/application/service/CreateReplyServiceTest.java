package com.newsfeed.engagement.application.service;

import com.newsfeed.common.error.ApiException;
import com.newsfeed.engagement.application.port.in.CreateReplyUseCase;
import com.newsfeed.engagement.application.port.out.EngagementEventPublisherPort;
import com.newsfeed.engagement.application.port.out.ReplyRepositoryPort;
import com.newsfeed.engagement.domain.Reply;
import com.newsfeed.engagement.domain.event.ReplyCreatedEvent;
import com.newsfeed.post.application.port.in.GetPostsByIdsUseCase;
import com.newsfeed.post.application.port.in.GetPostsByIdsUseCase.PostView;
import com.newsfeed.post.application.port.in.UpdatePostCountsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateReplyServiceTest {

    @Mock
    ReplyRepositoryPort replyRepository;
    @Mock
    GetPostsByIdsUseCase getPostsByIdsUseCase;
    @Mock
    UpdatePostCountsUseCase updatePostCountsUseCase;
    @Mock
    EngagementEventPublisherPort eventPublisher;

    @InjectMocks
    CreateReplyService service;

    @Test
    void 답글을_저장하고_횟수를_올리고_이벤트를_발행한다() {
        given(getPostsByIdsUseCase.list(List.of(100L)))
                .willReturn(List.of(new PostView(100L, 1L, "c", 0, 0, Instant.now())));
        given(replyRepository.save(org.mockito.ArgumentMatchers.any(Reply.class)))
                .willAnswer(inv -> {
                    Reply r = inv.getArgument(0);
                    return new Reply(500L, r.postId(), r.authorId(), r.content(), r.createdAt());
                });

        Reply result = service.create(new CreateReplyUseCase.Command(100L, 2L, "좋은 글이네요"));

        assertThat(result.id()).isEqualTo(500L);
        verify(updatePostCountsUseCase).incrementReplyCount(100L, 1);
        verify(eventPublisher).publish(new ReplyCreatedEvent(500L, 100L, 2L, result.createdAt()));
    }

    @Test
    void 존재하지_않는_포스트에_답글을_달면_404다() {
        given(getPostsByIdsUseCase.list(List.of(100L))).willReturn(List.of());

        assertThatThrownBy(() -> service.create(new CreateReplyUseCase.Command(100L, 2L, "x")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("POST_NOT_FOUND"));

        verify(replyRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(updatePostCountsUseCase, never()).incrementReplyCount(100L, 1);
    }
}
