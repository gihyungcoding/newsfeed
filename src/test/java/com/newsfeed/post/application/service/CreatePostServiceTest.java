package com.newsfeed.post.application.service;

import com.newsfeed.post.application.port.in.CreatePostUseCase;
import com.newsfeed.post.application.port.out.PostEventPublisherPort;
import com.newsfeed.post.application.port.out.PostRepositoryPort;
import com.newsfeed.post.domain.Post;
import com.newsfeed.post.domain.event.PostCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreatePostServiceTest {

    @Mock
    PostRepositoryPort postRepository;

    @Mock
    PostEventPublisherPort eventPublisher;

    @Captor
    ArgumentCaptor<Post> postCaptor;

    @InjectMocks
    CreatePostService createPostService;

    @Test
    void 포스트를_저장하고_생성_이벤트를_발행한다() {
        given(postRepository.save(postCaptor.capture()))
                .willAnswer(invocation -> {
                    Post toSave = invocation.getArgument(0);
                    return new Post(100L, toSave.authorId(), toSave.content(),
                            toSave.likeCount(), toSave.replyCount(), toSave.createdAt());
                });

        Post result = createPostService.create(new CreatePostUseCase.Command(1L, "첫 포스트"));

        assertThat(result.id()).isEqualTo(100L);
        assertThat(postCaptor.getValue().authorId()).isEqualTo(1L);
        assertThat(postCaptor.getValue().content()).isEqualTo("첫 포스트");

        verify(eventPublisher).publish(
                new PostCreatedEvent(100L, 1L, "첫 포스트", result.createdAt()));
    }
}
