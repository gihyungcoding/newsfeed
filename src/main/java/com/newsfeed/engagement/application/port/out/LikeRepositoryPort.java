package com.newsfeed.engagement.application.port.out;

/** 좋아요 관계 영속성 아웃바운드 포트. user 컨텍스트의 FollowRepositoryPort와 같은 모양. */
public interface LikeRepositoryPort {

    boolean exists(long postId, long userId);

    void insert(long postId, long userId);

    /** @return 실제로 삭제됐으면 true (멱등 취소 판정에 사용) */
    boolean delete(long postId, long userId);
}
