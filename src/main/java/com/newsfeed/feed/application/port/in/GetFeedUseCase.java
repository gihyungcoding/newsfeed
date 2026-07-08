package com.newsfeed.feed.application.port.in;

import java.time.Instant;
import java.util.List;

public interface GetFeedUseCase {

    FeedPage get(long userId, Long cursorEpochMillis, int size);

    record FeedPage(List<FeedItem> items, Long nextCursor) {
    }

    record FeedItem(long postId, String content, Instant createdAt, AuthorInfo author,
                    int likeCount, int replyCount, boolean likedByMe) {
    }

    /**
     * user 컨텍스트의 {@code GetUserSummaryUseCase.UserSummary}와 필드가 같지만 별도 타입으로 둔다 —
     * GetFeedUseCase를 읽는 사람이 이 타입의 모양을 알기 위해 user 패키지를 열어볼 필요가 없게 하기 위함
     * (ArchUnit이 요구하는 게 아니라 API 가독성을 위한 선택).
     */
    record AuthorInfo(long id, String username, String displayName) {
    }
}
