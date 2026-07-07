package com.newsfeed.user.application.service;

import com.newsfeed.common.error.ApiException;
import com.newsfeed.user.application.port.in.GetUserUseCase;
import com.newsfeed.user.application.port.out.UserCachePort;
import com.newsfeed.user.application.port.out.UserCountCachePort;
import com.newsfeed.user.application.port.out.UserRepositoryPort;
import com.newsfeed.user.domain.FollowCounts;
import com.newsfeed.user.domain.User;
import org.springframework.stereotype.Service;

@Service
public class GetUserService implements GetUserUseCase {

    private final UserRepositoryPort userRepository;
    private final UserCachePort userCache;
    private final UserCountCachePort countCache;

    public GetUserService(UserRepositoryPort userRepository,
                          UserCachePort userCache,
                          UserCountCachePort countCache) {
        this.userRepository = userRepository;
        this.userCache = userCache;
        this.countCache = countCache;
    }

    @Override
    public UserProfile get(long userId) {
        // look-aside: 캐시 조회 → miss면 DB 조회 → 캐시 적재
        User user = userCache.find(userId).orElseGet(() -> {
            User loaded = userRepository.findById(userId)
                    .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "사용자를 찾을 수 없습니다: " + userId));
            userCache.save(loaded);
            return loaded;
        });

        FollowCounts counts = countCache.find(userId).orElseGet(() -> {
            FollowCounts loaded = userRepository.findCounts(userId).orElse(FollowCounts.zero());
            countCache.save(userId, loaded);
            return loaded;
        });

        return new UserProfile(user, counts);
    }
}
