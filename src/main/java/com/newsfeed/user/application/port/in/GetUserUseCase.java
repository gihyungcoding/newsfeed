package com.newsfeed.user.application.port.in;

import com.newsfeed.user.domain.FollowCounts;
import com.newsfeed.user.domain.User;

public interface GetUserUseCase {

    /** 사용자 식별 정보 + 팔로우 횟수를 조립해 반환한다. 각각 별도 캐시 계층을 거친다. */
    UserProfile get(long userId);

    record UserProfile(User user, FollowCounts counts) {
    }
}
