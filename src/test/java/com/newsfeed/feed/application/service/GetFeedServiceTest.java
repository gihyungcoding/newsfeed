package com.newsfeed.feed.application.service;

import com.newsfeed.engagement.application.port.in.GetLikedPostIdsUseCase;
import com.newsfeed.feed.CelebrityThresholdProperties;
import com.newsfeed.feed.application.port.in.GetFeedUseCase;
import com.newsfeed.feed.application.port.out.FeedCacheReadPort;
import com.newsfeed.feed.domain.PushedPost;
import com.newsfeed.post.application.port.in.GetPostsByIdsUseCase;
import com.newsfeed.post.application.port.in.GetPostsByIdsUseCase.PostView;
import com.newsfeed.post.application.port.in.GetRecentPostsByAuthorUseCase;
import com.newsfeed.user.application.port.in.GetCelebrityFolloweesUseCase;
import com.newsfeed.user.application.port.in.GetUserSummaryUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetFeedServiceTest {

    @Mock
    FeedCacheReadPort feedCacheReadPort;
    @Mock
    GetCelebrityFolloweesUseCase getCelebrityFolloweesUseCase;
    @Mock
    GetRecentPostsByAuthorUseCase getRecentPostsByAuthorUseCase;
    @Mock
    GetPostsByIdsUseCase getPostsByIdsUseCase;
    @Mock
    GetUserSummaryUseCase getUserSummaryUseCase;
    @Mock
    GetLikedPostIdsUseCase getLikedPostIdsUseCase;

    @BeforeEach
    void setUp() {
        given(getLikedPostIdsUseCase.likedPostIds(anyList(), anyLong())).willReturn(Set.of());
    }

    private GetFeedService service(int celebrityThreshold) {
        return new GetFeedService(feedCacheReadPort, getCelebrityFolloweesUseCase, getRecentPostsByAuthorUseCase,
                getPostsByIdsUseCase, getUserSummaryUseCase, getLikedPostIdsUseCase,
                new CelebrityThresholdProperties(celebrityThreshold));
    }

    private PostView post(long id, long authorId, long createdAtMillis) {
        return new PostView(id, authorId, "content-" + id, 0, 0, Instant.ofEpochMilli(createdAtMillis));
    }

    private GetUserSummaryUseCase.UserSummary summary(long authorId) {
        return new GetUserSummaryUseCase.UserSummary(authorId, "user" + authorId, "User " + authorId);
    }

    @Test
    void push분과_celebrity_pull분을_작성시각_역순으로_병합한다() {
        given(feedCacheReadPort.readPushed(1L, null, 20))
                .willReturn(List.of(new PushedPost(100L, 3000L)));
        given(getPostsByIdsUseCase.list(List.of(100L)))
                .willReturn(List.of(post(100L, 2L, 3000L)));

        given(getCelebrityFolloweesUseCase.celebrityFolloweeIds(1L, 1000)).willReturn(List.of(9L));
        given(getRecentPostsByAuthorUseCase.recent(9L, 20))
                .willReturn(List.of(post(200L, 9L, 5000L))); // celebrity 포스트가 더 최신

        given(getUserSummaryUseCase.getSummary(2L)).willReturn(summary(2L));
        given(getUserSummaryUseCase.getSummary(9L)).willReturn(summary(9L));

        GetFeedUseCase.FeedPage page = service(1000).get(1L, null, 20);

        assertThat(page.items()).extracting(GetFeedUseCase.FeedItem::postId)
                .containsExactly(200L, 100L); // celebrity(5000)가 더 최신이므로 먼저
    }

    @Test
    void 같은_postId가_push와_pull_양쪽에_있으면_push분을_우선하고_중복_제거한다() {
        given(feedCacheReadPort.readPushed(1L, null, 20))
                .willReturn(List.of(new PushedPost(100L, 3000L)));
        given(getPostsByIdsUseCase.list(List.of(100L)))
                .willReturn(List.of(post(100L, 9L, 3000L)));

        given(getCelebrityFolloweesUseCase.celebrityFolloweeIds(1L, 1000)).willReturn(List.of(9L));
        // celebrity pull에도 같은 postId=100이 잡히는 경계 상황 (판정 시점 차이, §3.1)
        given(getRecentPostsByAuthorUseCase.recent(9L, 20)).willReturn(List.of(post(100L, 9L, 3000L)));

        given(getUserSummaryUseCase.getSummary(9L)).willReturn(summary(9L));

        GetFeedUseCase.FeedPage page = service(1000).get(1L, null, 20);

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).postId()).isEqualTo(100L);
    }

    @Test
    void 커서보다_최신인_celebrity_포스트는_제외한다() {
        given(feedCacheReadPort.readPushed(1L, 5000L, 20)).willReturn(List.of());
        given(getPostsByIdsUseCase.list(List.of())).willReturn(List.of());
        given(getCelebrityFolloweesUseCase.celebrityFolloweeIds(1L, 1000)).willReturn(List.of(9L));
        given(getRecentPostsByAuthorUseCase.recent(9L, 20)).willReturn(List.of(
                post(200L, 9L, 6000L), // cursor(5000)보다 최신 → 제외
                post(201L, 9L, 4000L)  // cursor보다 과거 → 포함
        ));
        given(getUserSummaryUseCase.getSummary(9L)).willReturn(summary(9L));

        GetFeedUseCase.FeedPage page = service(1000).get(1L, 5000L, 20);

        assertThat(page.items()).extracting(GetFeedUseCase.FeedItem::postId).containsExactly(201L);
    }

    @Test
    void 결과가_요청_size와_같으면_nextCursor를_채운다() {
        given(feedCacheReadPort.readPushed(1L, null, 1))
                .willReturn(List.of(new PushedPost(100L, 3000L)));
        given(getPostsByIdsUseCase.list(List.of(100L))).willReturn(List.of(post(100L, 2L, 3000L)));
        given(getCelebrityFolloweesUseCase.celebrityFolloweeIds(1L, 1000)).willReturn(List.of());
        given(getUserSummaryUseCase.getSummary(2L)).willReturn(summary(2L));

        GetFeedUseCase.FeedPage page = service(1000).get(1L, null, 1);

        assertThat(page.nextCursor()).isEqualTo(3000L);
    }
}
