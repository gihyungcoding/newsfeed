package com.newsfeed.user.application.port.in;

/**
 * 팔로워 수만 조회하는 좁은 유스케이스. 다른 컨텍스트(fanout)가 celebrity 판정을 위해
 * 프로필 전체({@link GetUserUseCase})가 아니라 이 인터페이스만 의존하도록 분리했다
 * (인터페이스 분리 원칙 — 호출자가 필요한 것만 보게 한다).
 *
 * <p>반환 타입을 도메인 객체 {@code FollowCounts}가 아니라 원시 타입 int로 둔 이유:
 * port.in의 반환 타입은 다른 컨텍스트가 그대로 받아 쓰는 공개 계약이 된다. 도메인 객체를
 * 그대로 반환하면 그 도메인 클래스가 컨텍스트 경계를 넘어 노출되어 버린다
 * (ArchUnit의 컨텍스트 경계 규칙이 이런 누출을 실제로 검출했다).
 */
public interface GetFollowCountsUseCase {

    int getFollowerCount(long userId);
}
