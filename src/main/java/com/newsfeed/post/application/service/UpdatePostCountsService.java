package com.newsfeed.post.application.service;

import com.newsfeed.post.application.port.in.UpdatePostCountsUseCase;
import com.newsfeed.post.application.port.out.PostEventPublisherPort;
import com.newsfeed.post.application.port.out.PostRepositoryPort;
import com.newsfeed.post.domain.event.PostCountsChangedEvent;
import org.springframework.stereotype.Service;

@Service
public class UpdatePostCountsService implements UpdatePostCountsUseCase {

    private final PostRepositoryPort postRepository;
    private final PostEventPublisherPort eventPublisher;

    public UpdatePostCountsService(PostRepositoryPort postRepository, PostEventPublisherPort eventPublisher) {
        this.postRepository = postRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void incrementLikeCount(long postId, int delta) {
        postRepository.incrementLikeCount(postId, delta);
        eventPublisher.publish(new PostCountsChangedEvent(postId, delta, 0));
    }

    @Override
    public void incrementReplyCount(long postId, int delta) {
        postRepository.incrementReplyCount(postId, delta);
        eventPublisher.publish(new PostCountsChangedEvent(postId, 0, delta));
    }
}
