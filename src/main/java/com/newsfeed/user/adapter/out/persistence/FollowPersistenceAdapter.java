package com.newsfeed.user.adapter.out.persistence;

import com.newsfeed.user.application.port.out.FollowRepositoryPort;
import org.springframework.stereotype.Component;

@Component
class FollowPersistenceAdapter implements FollowRepositoryPort {

    private final FollowJpaRepository followJpaRepository;

    FollowPersistenceAdapter(FollowJpaRepository followJpaRepository) {
        this.followJpaRepository = followJpaRepository;
    }

    @Override
    public boolean exists(long followerId, long followeeId) {
        return followJpaRepository.existsById_FollowerIdAndId_FolloweeId(followerId, followeeId);
    }

    @Override
    public void insert(long followerId, long followeeId) {
        followJpaRepository.save(new FollowJpaEntity(followerId, followeeId));
    }

    @Override
    public boolean delete(long followerId, long followeeId) {
        return followJpaRepository.deleteById_FollowerIdAndId_FolloweeId(followerId, followeeId) > 0;
    }
}
