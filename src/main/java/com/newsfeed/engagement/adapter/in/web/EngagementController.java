package com.newsfeed.engagement.adapter.in.web;

import com.newsfeed.common.auth.CurrentUserId;
import com.newsfeed.engagement.application.port.in.CreateReplyUseCase;
import com.newsfeed.engagement.application.port.in.GetRepliesUseCase;
import com.newsfeed.engagement.application.port.in.LikePostUseCase;
import com.newsfeed.engagement.domain.Reply;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
public class EngagementController {

    private final LikePostUseCase likePostUseCase;
    private final CreateReplyUseCase createReplyUseCase;
    private final GetRepliesUseCase getRepliesUseCase;

    public EngagementController(LikePostUseCase likePostUseCase,
                                CreateReplyUseCase createReplyUseCase,
                                GetRepliesUseCase getRepliesUseCase) {
        this.likePostUseCase = likePostUseCase;
        this.createReplyUseCase = createReplyUseCase;
        this.getRepliesUseCase = getRepliesUseCase;
    }

    @PostMapping("/api/posts/{postId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void like(@PathVariable long postId, @CurrentUserId Long userId) {
        likePostUseCase.like(postId, userId);
    }

    @DeleteMapping("/api/posts/{postId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unlike(@PathVariable long postId, @CurrentUserId Long userId) {
        likePostUseCase.unlike(postId, userId);
    }

    @PostMapping("/api/posts/{postId}/replies")
    ResponseEntity<ReplyResponse> createReply(@PathVariable long postId,
                                              @CurrentUserId Long authorId,
                                              @Validated @RequestBody CreateReplyRequest request) {
        Reply reply = createReplyUseCase.create(new CreateReplyUseCase.Command(postId, authorId, request.content()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ReplyResponse.of(reply));
    }

    @GetMapping("/api/posts/{postId}/replies")
    ReplyListResponse listReplies(@PathVariable long postId,
                                  @RequestParam(required = false) Long cursor,
                                  @RequestParam(defaultValue = "20") int size) {
        GetRepliesUseCase.PagedReplies paged = getRepliesUseCase.list(postId, cursor, size);
        List<ReplyResponse> items = paged.items().stream().map(ReplyResponse::of).toList();
        return new ReplyListResponse(items, paged.nextCursor());
    }

    record CreateReplyRequest(@NotBlank @Size(max = 300) String content) {
    }

    record ReplyResponse(long id, long postId, long authorId, String content, Instant createdAt) {
        static ReplyResponse of(Reply reply) {
            return new ReplyResponse(reply.id(), reply.postId(), reply.authorId(), reply.content(), reply.createdAt());
        }
    }

    record ReplyListResponse(List<ReplyResponse> items, Long nextCursor) {
    }
}
