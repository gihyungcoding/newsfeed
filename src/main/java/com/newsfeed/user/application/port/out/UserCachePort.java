package com.newsfeed.user.application.port.out;

import com.newsfeed.user.domain.User;

import java.util.Optional;

/** 사용자 식별 정보 캐시 (user:{id}, look-aside). 콘텐츠 조립 시 작성자 정보로 재사용된다. */
public interface UserCachePort {

    Optional<User> find(long userId);

    void save(User user);
}
