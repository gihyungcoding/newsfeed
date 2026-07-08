package com.newsfeed.engagement.application.port.out;

/**
 * 좋아요 행동 캐시 (post-likers:{postId}, Redis Set). 책의 5계층 중 "행동" 계층.
 *
 * <p>{@code exists}와 {@code isMember}를 분리한 이유: Redis Set이 비어 있는 것과 캐시 키
 * 자체가 없는 것(TTL 만료 등 콜드 상태)을 구분해야 한다. 후자일 때만 DB로 fallback해야
 * "그 포스트를 아무도 안 좋아함"과 "캐시가 아직 안 채워짐"을 혼동하지 않는다.
 */
public interface PostLikersCachePort {

    void add(long postId, long userId);

    void remove(long postId, long userId);

    /** 캐시 키(Set) 자체가 존재하는지 — false면 콜드 상태이니 호출자가 DB로 판정해야 한다. */
    boolean exists(long postId);

    /** exists(postId)가 true일 때만 신뢰할 수 있는 결과다. */
    boolean isMember(long postId, long userId);
}
