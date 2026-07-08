package com.newsfeed.engagement.application.port.in;

import java.util.List;
import java.util.Set;

/**
 * 주어진 postId들 중 userId가 좋아요한 것의 부분집합을 돌려준다. feed 컨텍스트가 각 피드
 * 아이템의 likedByMe를 조립할 때 쓰는 좁은 유스케이스 — post-likers 캐시나 post_likes
 * 테이블 자체는 노출하지 않는다.
 */
public interface GetLikedPostIdsUseCase {

    Set<Long> likedPostIds(List<Long> postIds, long userId);
}
