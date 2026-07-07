package com.newsfeed.user.application.service;

import com.newsfeed.user.application.port.in.GetFollowerIdsUseCase;
import com.newsfeed.user.application.port.out.FollowRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FollowQueryService implements GetFollowerIdsUseCase {

    private final FollowRepositoryPort followRepository;

    public FollowQueryService(FollowRepositoryPort followRepository) {
        this.followRepository = followRepository;
    }

    @Override
    public List<Long> page(long followeeId, Long afterFollowerId, int limit) {
        return followRepository.findFollowerIds(followeeId, afterFollowerId, limit);
    }
}
