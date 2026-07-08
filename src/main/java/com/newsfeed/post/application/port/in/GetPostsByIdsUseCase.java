package com.newsfeed.post.application.port.in;

import com.newsfeed.post.domain.Post;
import com.newsfeed.post.domain.PostCounts;

import java.time.Instant;
import java.util.List;

/**
 * postId 목록을 받아 콘텐츠 + 횟수를 조립한다 (look-aside: 각각의 캐시 우선, miss만 DB 조회).
 * feed 컨텍스트가 피드 아이템을 조립할 때 쓰는 좁은 유스케이스.
 *
 * <p>반환 타입이 {@code post.domain.Post}가 아니라 이 인터페이스의 {@link PostView}인 이유는
 * user 컨텍스트의 {@code GetFollowCountsUseCase}/{@code GetUserSummaryUseCase}와 같다 —
 * port.in의 반환 타입은 다른 컨텍스트가 그대로 받아 쓰는 공개 계약이므로 도메인 객체를
 * 노출하지 않는다.
 */
public interface GetPostsByIdsUseCase {

    /** 반환 순서는 입력 순서와 무관하다 — 호출자가 필요한 기준(작성시각 등)으로 다시 정렬한다. */
    List<PostView> list(List<Long> postIds);

    record PostView(long id, long authorId, String content, int likeCount, int replyCount, Instant createdAt) {

        public static PostView of(Post post, PostCounts counts) {
            return new PostView(post.id(), post.authorId(), post.content(),
                    counts.likeCount(), counts.replyCount(), post.createdAt());
        }
    }
}
