package com.newsfeed.user.domain;

/** 팔로워/팔로잉 횟수 값 객체. 원천은 follows 테이블의 행이며 이 값은 파생값이다. */
public record FollowCounts(int followerCount, int followingCount) {

    public static FollowCounts zero() {
        return new FollowCounts(0, 0);
    }
}
