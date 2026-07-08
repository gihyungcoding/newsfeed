package com.newsfeed.post.domain;

/** 좋아요/답글 횟수 값 객체. 원천은 post_likes/replies 테이블의 행이며 이 값은 파생값이다. */
public record PostCounts(int likeCount, int replyCount) {

    public static PostCounts zero() {
        return new PostCounts(0, 0);
    }
}
