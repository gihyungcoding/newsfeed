package com.newsfeed.user.application.port.in;

/**
 * 다른 컨텍스트(feed)가 피드 아이템의 작성자 정보를 조립할 때 쓰는 좁은 유스케이스.
 *
 * <p>반환 타입이 도메인 객체 {@code User}가 아니라 이 인터페이스 안에 정의된 {@link UserSummary}인
 * 이유는 {@link GetFollowCountsUseCase}와 같다 — port.in의 반환 타입은 다른 컨텍스트가 그대로
 * 받아 쓰는 공개 계약이므로, 도메인 객체를 그대로 반환하면 컨텍스트 경계가 새어 나간다.
 */
public interface GetUserSummaryUseCase {

    UserSummary getSummary(long userId);

    record UserSummary(long id, String username, String displayName) {
    }
}
