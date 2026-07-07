package com.newsfeed.user.application.port.in;

public interface FollowUseCase {

    /** followerId가 targetId를 팔로우한다. 자기 자신/중복 팔로우는 거부된다. */
    void follow(long followerId, long targetId);

    /** 언팔로우. 멱등 — 팔로우 상태가 아니어도 오류가 아니다. */
    void unfollow(long followerId, long targetId);
}
