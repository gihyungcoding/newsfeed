package com.newsfeed.post.application.port.in;

import java.util.List;

/**
 * 작성자의 최근 포스트를 pull한다 — 하이브리드 팬아웃에서 celebrity의 포스트를 조회 시점에
 * 가져오는 경로(fanout-on-read)에 쓰인다 (docs/03-detailed-design.md §3.4).
 */
public interface GetRecentPostsByAuthorUseCase {

    List<GetPostsByIdsUseCase.PostView> recent(long authorId, int limit);
}
