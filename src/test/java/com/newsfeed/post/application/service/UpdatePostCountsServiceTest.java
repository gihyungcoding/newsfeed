package com.newsfeed.post.application.service;

import com.newsfeed.post.application.port.out.PostEventPublisherPort;
import com.newsfeed.post.application.port.out.PostRepositoryPort;
import com.newsfeed.post.domain.event.PostCountsChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class UpdatePostCountsServiceTest {

    @Mock
    PostRepositoryPort postRepository;
    @Mock
    PostEventPublisherPort eventPublisher;

    @InjectMocks
    UpdatePostCountsService service;

    @Test
    void 좋아요_횟수를_DB에서_먼저_증감한_뒤_이벤트를_발행한다() {
        service.incrementLikeCount(100L, 1);

        InOrder order = inOrder(postRepository, eventPublisher);
        order.verify(postRepository).incrementLikeCount(100L, 1);
        order.verify(eventPublisher).publish(new PostCountsChangedEvent(100L, 1, 0));
    }

    @Test
    void 답글_횟수_감소도_같은_순서로_처리한다() {
        service.incrementReplyCount(100L, -1);

        InOrder order = inOrder(postRepository, eventPublisher);
        order.verify(postRepository).incrementReplyCount(100L, -1);
        order.verify(eventPublisher).publish(new PostCountsChangedEvent(100L, 0, -1));
    }
}
