package com.newsfeed.user.application.service;

import com.newsfeed.common.error.ApiException;
import com.newsfeed.user.application.port.in.GetFollowCountsUseCase;
import com.newsfeed.user.application.port.in.GetUserSummaryUseCase;
import com.newsfeed.user.application.port.in.GetUserUseCase;
import com.newsfeed.user.application.port.out.UserCachePort;
import com.newsfeed.user.application.port.out.UserCountCachePort;
import com.newsfeed.user.application.port.out.UserRepositoryPort;
import com.newsfeed.user.domain.FollowCounts;
import com.newsfeed.user.domain.User;
import org.springframework.stereotype.Service;

@Service
public class GetUserService implements GetUserUseCase, GetFollowCountsUseCase, GetUserSummaryUseCase {

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
        return new UserProfile(loadUser(userId), loadCounts(userId));
    }

    @Override
    public int getFollowerCount(long userId) {
        return loadCounts(userId).followerCount();
    }

    @Override
    public UserSummary getSummary(long userId) {
        User user = loadUser(userId);
        return new UserSummary(user.id(), user.username(), user.displayName());
    }

    // look-aside: 캐시 조회 → miss면 DB 조회 → 캐시 적재. get()과 getSummary()가 공유한다.
    // User(도메인 객체)는 이 클래스 밖으로 절대 나가지 않는다 — 다른 컨텍스트에는
    // getSummary()가 반환하는 원시 필드 조합(UserSummary)만 노출한다.
    private User loadUser(long userId) {
        return userCache.find(userId).orElseGet(() -> {
            User loaded = userRepository.findById(userId)
                    .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "사용자를 찾을 수 없습니다: " + userId));
            userCache.save(loaded);
            return loaded;
        });
    }

    // look-aside: 프로필 조회와 같은 캐시(cnt:user:{id})를 공유한다 — 팬아웃의 celebrity
    // 판정처럼 프로필 전체가 필요 없는 호출도 같은 캐시를 재사용해 별도 적재를 피한다.
    private FollowCounts loadCounts(long userId) {
        return countCache.find(userId).orElseGet(() -> {
            FollowCounts loaded = userRepository.findCounts(userId).orElse(FollowCounts.zero());
            countCache.save(userId, loaded);
            return loaded;
        });
    }
}
