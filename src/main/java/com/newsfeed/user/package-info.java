/**
 * user 컨텍스트 — 사용자, 팔로우 관계, 소셜 그래프 캐시.
 *
 * <p>내부 구조는 클린 아키텍처 계층을 따른다:
 * domain(순수 도메인) ← application(유스케이스 + 포트) ← adapter(web/persistence/redis).
 * 다른 컨텍스트는 이 컨텍스트의 {@code application.port.in}과 도메인 이벤트로만 접근할 수 있다.
 */
package com.newsfeed.user;
