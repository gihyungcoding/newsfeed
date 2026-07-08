package com.newsfeed.engagement.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 답글 도메인 모델. 좋아요는 "존재 여부"만 있는 순수 관계라 별도 도메인 객체를 두지 않았다
 * (user 컨텍스트가 팔로우 관계에 Follow 도메인 객체를 두지 않은 것과 같은 이유) — 답글은
 * content라는 실제 값을 갖는 엔티티라 도메인 객체가 필요하다.
 */
public record Reply(Long id, long postId, long authorId, String content, Instant createdAt) {

    public Reply {
        if (content == null || content.isBlank() || content.length() > 300) {
            throw new IllegalArgumentException("content는 1~300자여야 합니다");
        }
    }

    public static Reply create(long postId, long authorId, String content) {
        return new Reply(null, postId, authorId, content, Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }
}
