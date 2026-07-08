package com.newsfeed.engagement.adapter.out.persistence;

import com.newsfeed.engagement.application.port.out.ReplyRepositoryPort;
import com.newsfeed.engagement.domain.Reply;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
class ReplyPersistenceAdapter implements ReplyRepositoryPort {

    private final ReplyJpaRepository replyJpaRepository;

    ReplyPersistenceAdapter(ReplyJpaRepository replyJpaRepository) {
        this.replyJpaRepository = replyJpaRepository;
    }

    @Override
    public Reply save(Reply reply) {
        return replyJpaRepository.save(ReplyJpaEntity.from(reply)).toDomain();
    }

    @Override
    public List<Reply> findByPost(long postId, Instant cursor, int size) {
        return replyJpaRepository.findByPost(postId, cursor, PageRequest.of(0, size))
                .stream().map(ReplyJpaEntity::toDomain).toList();
    }

    @Override
    public List<Reply> findAllByIds(List<Long> replyIds) {
        return replyJpaRepository.findAllById(replyIds).stream().map(ReplyJpaEntity::toDomain).toList();
    }
}
