package com.newsfeed.post.application.port.in;

/**
 * post의 좋아요/답글 횟수를 증감한다. engagement 컨텍스트가 좋아요/답글을 처리할 때
 * post의 도메인 데이터(posts.like_count/reply_count, cnt:post 캐시)를 직접 건드리지 않고
 * 이 유스케이스만 호출한다 — fanout이 user의 카운트를 port.in으로만 조회하는 것과 같은 경계 원칙을
 * 쓰기 방향에도 적용한 것이다.
 *
 * <p>이 메서드는 별도 트랜잭션을 열지 않는다(전파 기본값 REQUIRED) — 호출자(engagement)의
 * 트랜잭션에 그대로 참여해, "좋아요 행 삽입"과 "횟수 증가"가 원자적으로 함께 커밋되거나
 * 함께 롤백된다.
 */
public interface UpdatePostCountsUseCase {

    void incrementLikeCount(long postId, int delta);

    void incrementReplyCount(long postId, int delta);
}
