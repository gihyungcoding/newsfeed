package com.newsfeed.user.adapter.in.web;

import com.newsfeed.common.auth.CurrentUserId;
import com.newsfeed.user.application.port.in.CreateUserUseCase;
import com.newsfeed.user.application.port.in.FollowUseCase;
import com.newsfeed.user.application.port.in.GetUserUseCase;
import com.newsfeed.user.domain.FollowCounts;
import com.newsfeed.user.domain.User;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * 웹 어댑터(인바운드). 유스케이스 인터페이스(port/in)에만 의존하며,
 * HTTP ↔ 유스케이스 사이의 변환만 담당한다 — 비즈니스 규칙은 여기 두지 않는다.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final GetUserUseCase getUserUseCase;
    private final FollowUseCase followUseCase;

    public UserController(CreateUserUseCase createUserUseCase,
                          GetUserUseCase getUserUseCase,
                          FollowUseCase followUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.getUserUseCase = getUserUseCase;
        this.followUseCase = followUseCase;
    }

    @PostMapping
    ResponseEntity<UserResponse> create(@Validated @RequestBody CreateUserRequest request) {
        User user = createUserUseCase.create(
                new CreateUserUseCase.Command(request.username(), request.displayName()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponse.of(user, FollowCounts.zero()));
    }

    @GetMapping("/{userId}")
    UserResponse get(@PathVariable long userId) {
        GetUserUseCase.UserProfile profile = getUserUseCase.get(userId);
        return UserResponse.of(profile.user(), profile.counts());
    }

    @PostMapping("/{targetUserId}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void follow(@PathVariable long targetUserId, @CurrentUserId Long currentUserId) {
        followUseCase.follow(currentUserId, targetUserId);
    }

    @DeleteMapping("/{targetUserId}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unfollow(@PathVariable long targetUserId, @CurrentUserId Long currentUserId) {
        followUseCase.unfollow(currentUserId, targetUserId);
    }

    record CreateUserRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(max = 100) String displayName) {
    }

    record UserResponse(long id, String username, String displayName,
                        int followerCount, int followingCount, Instant createdAt) {

        static UserResponse of(User user, FollowCounts counts) {
            return new UserResponse(user.id(), user.username(), user.displayName(),
                    counts.followerCount(), counts.followingCount(), user.createdAt());
        }
    }
}
