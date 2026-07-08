package com.newsfeed.feed.adapter.in.web;

import com.newsfeed.common.auth.CurrentUserId;
import com.newsfeed.feed.application.port.in.GetFeedUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
public class FeedController {

    private final GetFeedUseCase getFeedUseCase;

    public FeedController(GetFeedUseCase getFeedUseCase) {
        this.getFeedUseCase = getFeedUseCase;
    }

    @GetMapping("/api/feed")
    FeedResponse getFeed(@CurrentUserId Long userId,
                        @RequestParam(required = false) Long cursor,
                        @RequestParam(defaultValue = "20") int size) {
        GetFeedUseCase.FeedPage page = getFeedUseCase.get(userId, cursor, size);
        List<FeedItemResponse> items = page.items().stream().map(FeedItemResponse::of).toList();
        return new FeedResponse(items, page.nextCursor());
    }

    record AuthorResponse(long id, String username, String displayName) {
        static AuthorResponse of(GetFeedUseCase.AuthorInfo author) {
            return new AuthorResponse(author.id(), author.username(), author.displayName());
        }
    }

    record FeedItemResponse(long postId, String content, Instant createdAt, AuthorResponse author,
                            int likeCount, int replyCount, boolean likedByMe) {
        static FeedItemResponse of(GetFeedUseCase.FeedItem item) {
            return new FeedItemResponse(item.postId(), item.content(), item.createdAt(),
                    AuthorResponse.of(item.author()), item.likeCount(), item.replyCount(), item.likedByMe());
        }
    }

    record FeedResponse(List<FeedItemResponse> items, Long nextCursor) {
    }
}
