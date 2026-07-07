package com.newsfeed.user.application.port.out;

/**
 * 소셜 그래프 캐시 (following:{userId}, Redis Set). 책의 5계층 중 "소셜 그래프" 계층.
 * 조회(피드 컨텍스트에서 celebrity 필터링)는 5단계에서 추가된다 — 지금은 무효화만 필요하다.
 */
public interface FollowingCachePort {

    /** 팔로우 관계가 바뀌면 목록 캐시를 무효화한다. 다음 조회 때 DB에서 재적재된다. */
    void evict(long userId);
}
