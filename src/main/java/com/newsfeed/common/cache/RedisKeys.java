package com.newsfeed.common.cache;

/**
 * 모든 Redis 키 패턴을 한곳에 모은다 (docs/03-detailed-design.md §3.2 캐시 카탈로그).
 * 어댑터마다 키 문자열을 하드코딩하면 오타 버그와 "이 키를 누가 쓰는지" 추적 불가 문제가
 * 생기므로, 키 조립은 항상 이 클래스를 거치도록 강제한다.
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 뉴스피드 캐시 (Sorted Set, member=postId, score=작성시각). */
    public static String feed(long userId) {
        return "feed:" + userId;
    }

    /** 사용자 프로필 캐시 (Hash). */
    public static String userProfile(long userId) {
        return "user:" + userId;
    }

    /** 팔로워/팔로잉 횟수 캐시 (Hash {followers, following}). */
    public static String userCounts(long userId) {
        return "cnt:user:" + userId;
    }

    /** 소셜 그래프 캐시: 팔로잉 목록 (Set). */
    public static String following(long userId) {
        return "following:" + userId;
    }

    /** 포스트 콘텐츠 캐시 (String, JSON). */
    public static String post(long postId) {
        return "post:" + postId;
    }

    /** celebrity 최근 포스트 목록 (Sorted Set) — fanout-on-read pull 경로용. */
    public static String authorPosts(long authorId) {
        return "author-posts:" + authorId;
    }

    /** 좋아요 누른 사용자 목록 (Set). */
    public static String postLikers(long postId) {
        return "post-likers:" + postId;
    }

    /** 답글 ID 목록 (Sorted Set). */
    public static String postReplies(long postId) {
        return "post-replies:" + postId;
    }

    /** 좋아요/답글 횟수 캐시 (Hash {likes, replies}). */
    public static String postCounts(long postId) {
        return "cnt:post:" + postId;
    }

    /** 처리율 제한 고정 윈도 카운터 (String). */
    public static String rateLimit(long userId, long windowEpochSeconds) {
        return "ratelimit:%d:%d".formatted(userId, windowEpochSeconds);
    }
}
