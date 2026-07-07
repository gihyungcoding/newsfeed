package com.newsfeed.post.adapter.out.persistence;

import com.newsfeed.post.application.port.out.PostRepositoryPort;
import com.newsfeed.post.domain.Post;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
class PostPersistenceAdapter implements PostRepositoryPort {

    private final PostJpaRepository postJpaRepository;

    PostPersistenceAdapter(PostJpaRepository postJpaRepository) {
        this.postJpaRepository = postJpaRepository;
    }

    @Override
    public Post save(Post post) {
        return postJpaRepository.save(PostJpaEntity.from(post)).toDomain();
    }

    @Override
    public Optional<Post> findById(long postId) {
        return postJpaRepository.findById(postId).map(PostJpaEntity::toDomain);
    }

    @Override
    public List<Post> findByAuthor(long authorId, Instant cursor, int size) {
        return postJpaRepository.findByAuthor(authorId, cursor, PageRequest.of(0, size))
                .stream().map(PostJpaEntity::toDomain).toList();
    }

    @Override
    public List<Post> findAllByIds(List<Long> postIds) {
        return postJpaRepository.findAllById(postIds).stream().map(PostJpaEntity::toDomain).toList();
    }
}
