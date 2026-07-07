package com.newsfeed.post.application.service;

import com.newsfeed.post.application.port.in.CreatePostUseCase;
import com.newsfeed.post.application.port.out.PostEventPublisherPort;
import com.newsfeed.post.application.port.out.PostRepositoryPort;
import com.newsfeed.post.domain.Post;
import com.newsfeed.post.domain.event.PostCreatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 포스트 발행 유스케이스. DB 저장까지만 트랜잭션으로 묶고, 캐시 적재와 Kafka 발행은
 * PostCreatedEvent 구독자들이 커밋 후(AFTER_COMMIT) 처리한다 — user 컨텍스트의
 * FollowService와 같은 이유(롤백된 발행이 캐시/이벤트로 새어 나가지 않게 하기 위함)다.
 */
@Service
public class CreatePostService implements CreatePostUseCase {

    private final PostRepositoryPort postRepository;
    private final PostEventPublisherPort eventPublisher;

    public CreatePostService(PostRepositoryPort postRepository, PostEventPublisherPort eventPublisher) {
        this.postRepository = postRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Post create(Command command) {
        Post saved = postRepository.save(Post.create(command.authorId(), command.content()));
        eventPublisher.publish(new PostCreatedEvent(saved.id(), saved.authorId(), saved.content(), saved.createdAt()));
        return saved;
    }
}
