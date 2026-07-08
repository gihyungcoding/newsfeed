package com.newsfeed.engagement.application.service;

import com.newsfeed.common.error.ApiException;
import com.newsfeed.engagement.application.port.in.CreateReplyUseCase;
import com.newsfeed.engagement.application.port.out.EngagementEventPublisherPort;
import com.newsfeed.engagement.application.port.out.ReplyRepositoryPort;
import com.newsfeed.engagement.domain.Reply;
import com.newsfeed.engagement.domain.event.ReplyCreatedEvent;
import com.newsfeed.post.application.port.in.GetPostsByIdsUseCase;
import com.newsfeed.post.application.port.in.UpdatePostCountsUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CreateReplyService implements CreateReplyUseCase {

    private final ReplyRepositoryPort replyRepository;
    private final GetPostsByIdsUseCase getPostsByIdsUseCase;
    private final UpdatePostCountsUseCase updatePostCountsUseCase;
    private final EngagementEventPublisherPort eventPublisher;

    public CreateReplyService(ReplyRepositoryPort replyRepository,
                              GetPostsByIdsUseCase getPostsByIdsUseCase,
                              UpdatePostCountsUseCase updatePostCountsUseCase,
                              EngagementEventPublisherPort eventPublisher) {
        this.replyRepository = replyRepository;
        this.getPostsByIdsUseCase = getPostsByIdsUseCase;
        this.updatePostCountsUseCase = updatePostCountsUseCase;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Reply create(Command command) {
        if (getPostsByIdsUseCase.list(List.of(command.postId())).isEmpty()) {
            throw ApiException.notFound("POST_NOT_FOUND", "포스트를 찾을 수 없습니다: " + command.postId());
        }
        Reply saved = replyRepository.save(Reply.create(command.postId(), command.authorId(), command.content()));
        updatePostCountsUseCase.incrementReplyCount(command.postId(), 1);
        eventPublisher.publish(new ReplyCreatedEvent(saved.id(), saved.postId(), saved.authorId(), saved.createdAt()));
        return saved;
    }
}
