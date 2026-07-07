package com.newsfeed.post.adapter.in.web;

import com.newsfeed.common.auth.CurrentUserId;
import com.newsfeed.post.application.port.in.CreatePostUseCase;
import com.newsfeed.post.application.port.in.GetUserPostsUseCase;
import com.newsfeed.post.domain.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
public class PostController {

    private final CreatePostUseCase createPostUseCase;
    private final GetUserPostsUseCase getUserPostsUseCase;

    public PostController(CreatePostUseCase createPostUseCase, GetUserPostsUseCase getUserPostsUseCase) {
        this.createPostUseCase = createPostUseCase;
        this.getUserPostsUseCase = getUserPostsUseCase;
    }

    @PostMapping("/api/posts")
    ResponseEntity<PostResponse> create(@CurrentUserId Long authorId,
                                        @Validated @RequestBody CreatePostRequest request) {
        Post post = createPostUseCase.create(new CreatePostUseCase.Command(authorId, request.content()));
        return ResponseEntity.status(HttpStatus.CREATED).body(PostResponse.of(post));
    }

    @GetMapping("/api/users/{userId}/posts")
    PostListResponse listByAuthor(@PathVariable long userId,
                                  @RequestParam(required = false) Long cursor,
                                  @RequestParam(defaultValue = "20") int size) {
        GetUserPostsUseCase.PagedPosts paged = getUserPostsUseCase.list(userId, cursor, size);
        List<PostResponse> items = paged.items().stream().map(PostResponse::of).toList();
        return new PostListResponse(items, paged.nextCursor());
    }

    record CreatePostRequest(@NotBlank @Size(max = 500) String content) {
    }

    record PostResponse(long id, long authorId, String content, Instant createdAt) {
        static PostResponse of(Post post) {
            return new PostResponse(post.id(), post.authorId(), post.content(), post.createdAt());
        }
    }

    record PostListResponse(List<PostResponse> items, Long nextCursor) {
    }
}
