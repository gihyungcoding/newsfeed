package com.newsfeed.engagement.adapter.out.persistence;

import com.newsfeed.engagement.application.port.out.LikeRepositoryPort;
import org.springframework.stereotype.Component;

@Component
class LikePersistenceAdapter implements LikeRepositoryPort {

    private final LikeJpaRepository likeJpaRepository;

    LikePersistenceAdapter(LikeJpaRepository likeJpaRepository) {
        this.likeJpaRepository = likeJpaRepository;
    }

    @Override
    public boolean exists(long postId, long userId) {
        return likeJpaRepository.existsById_PostIdAndId_UserId(postId, userId);
    }

    @Override
    public void insert(long postId, long userId) {
        // 즉시 flush해서 PK 위반(동시 중복 좋아요)이 호출 지점에서 예외로 드러나게 한다 (FollowPersistenceAdapter와 동일)
        likeJpaRepository.saveAndFlush(new LikeJpaEntity(postId, userId));
    }

    @Override
    public boolean delete(long postId, long userId) {
        return likeJpaRepository.deleteById_PostIdAndId_UserId(postId, userId) > 0;
    }
}
