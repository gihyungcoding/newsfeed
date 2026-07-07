package com.newsfeed.user.adapter.out.persistence;

import com.newsfeed.user.application.port.out.UserRepositoryPort;
import com.newsfeed.user.domain.FollowCounts;
import com.newsfeed.user.domain.User;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;

    UserPersistenceAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public User save(User user) {
        return userJpaRepository.save(UserJpaEntity.from(user)).toDomain();
    }

    @Override
    public Optional<User> findById(long userId) {
        return userJpaRepository.findById(userId).map(UserJpaEntity::toDomain);
    }

    @Override
    public boolean existsById(long userId) {
        return userJpaRepository.existsById(userId);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userJpaRepository.existsByUsername(username);
    }

    @Override
    public Optional<FollowCounts> findCounts(long userId) {
        return userJpaRepository.findById(userId)
                .map(e -> new FollowCounts(e.getFollowerCount(), e.getFollowingCount()));
    }

    @Override
    public void incrementFollowerCount(long userId, int delta) {
        userJpaRepository.incrementFollowerCount(userId, delta);
    }

    @Override
    public void incrementFollowingCount(long userId, int delta) {
        userJpaRepository.incrementFollowingCount(userId, delta);
    }
}
