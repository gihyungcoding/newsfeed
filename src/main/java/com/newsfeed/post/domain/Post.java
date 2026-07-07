package com.newsfeed.post.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** 포스트 도메인 모델. 좋아요/답글 횟수는 engagement 컨텍스트가 갱신하는 파생 컬럼이다. */
public record Post(Long id, long authorId, String content, int likeCount, int replyCount, Instant createdAt) {

    public Post {
        if (content == null || content.isBlank() || content.length() > 500) {
            throw new IllegalArgumentException("content는 1~500자여야 합니다");
        }
    }

    public static Post create(long authorId, String content) {
        return new Post(null, authorId, content, 0, 0, Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }
}
