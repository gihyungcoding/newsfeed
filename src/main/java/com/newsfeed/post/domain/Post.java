package com.newsfeed.post.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 포스트 도메인 모델. 좋아요/답글 횟수는 여기 없다 — content(불변)와 counts(자주 변함)는
 * 캐시 갱신 빈도가 근본적으로 달라 별도 타입({@link PostCounts})으로 분리했다
 * (user 컨텍스트의 User/FollowCounts 분리와 같은 이유, docs/03-detailed-design.md §3.2).
 */
public record Post(Long id, long authorId, String content, Instant createdAt) {

    public Post {
        if (content == null || content.isBlank() || content.length() > 500) {
            throw new IllegalArgumentException("content는 1~500자여야 합니다");
        }
    }

    public static Post create(long authorId, String content) {
        return new Post(null, authorId, content, Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }
}
