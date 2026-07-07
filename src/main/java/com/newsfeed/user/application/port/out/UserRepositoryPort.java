package com.newsfeed.user.application.port.out;

import com.newsfeed.user.domain.FollowCounts;
import com.newsfeed.user.domain.User;

import java.util.Optional;

/** 사용자 영속성 아웃바운드 포트. JPA 어댑터가 구현한다. */
public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(long userId);

    boolean existsById(long userId);

    boolean existsByUsername(String username);

    Optional<FollowCounts> findCounts(long userId);

    /** users.follower_count를 원자적으로 증감한다 (UPDATE ... SET x = x + delta). */
    void incrementFollowerCount(long userId, int delta);

    void incrementFollowingCount(long userId, int delta);
}
