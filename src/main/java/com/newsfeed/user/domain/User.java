package com.newsfeed.user.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 사용자 도메인 모델. 프레임워크에 의존하지 않는 순수 Java다 (ArchUnit으로 강제).
 * 팔로워/팔로잉 횟수는 자주 변하는 파생값이므로 별도 값 객체 {@link FollowCounts}로 분리했다
 * — 이 객체는 불변 식별 정보만 담아 캐시 정합성 문제를 단순화한다.
 */
public record User(Long id, String username, String displayName, Instant createdAt) {

    public User {
        if (username == null || username.isBlank() || username.length() > 50) {
            throw new IllegalArgumentException("username은 1~50자여야 합니다");
        }
        if (displayName == null || displayName.isBlank() || displayName.length() > 100) {
            throw new IllegalArgumentException("displayName은 1~100자여야 합니다");
        }
    }

    /** 신규 사용자 생성. id는 저장소가 부여한다. TIMESTAMP(3) 컬럼에 맞춰 밀리초로 절단한다. */
    public static User create(String username, String displayName) {
        return new User(null, username, displayName, Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }
}
