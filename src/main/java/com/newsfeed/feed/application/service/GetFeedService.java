package com.newsfeed.feed.application.service;

import com.newsfeed.feed.CelebrityThresholdProperties;
import com.newsfeed.feed.application.port.in.GetFeedUseCase;
import com.newsfeed.feed.application.port.out.FeedCacheReadPort;
import com.newsfeed.feed.domain.PushedPost;
import com.newsfeed.post.application.port.in.GetPostsByIdsUseCase;
import com.newsfeed.post.application.port.in.GetPostsByIdsUseCase.PostView;
import com.newsfeed.post.application.port.in.GetRecentPostsByAuthorUseCase;
import com.newsfeed.user.application.port.in.GetCelebrityFolloweesUseCase;
import com.newsfeed.user.application.port.in.GetUserSummaryUseCase;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 하이브리드 뉴스피드 조회 (docs/03-detailed-design.md §3.4).
 * push분(feed:{userId} 캐시)과 celebrity pull분을 postId 기준으로 합쳐 최신순으로 자른다.
 */
@Service
public class GetFeedService implements GetFeedUseCase {

    private final FeedCacheReadPort feedCacheReadPort;
    private final GetCelebrityFolloweesUseCase getCelebrityFolloweesUseCase;
    private final GetRecentPostsByAuthorUseCase getRecentPostsByAuthorUseCase;
    private final GetPostsByIdsUseCase getPostsByIdsUseCase;
    private final GetUserSummaryUseCase getUserSummaryUseCase;
    private final CelebrityThresholdProperties celebrityThresholdProperties;

    public GetFeedService(FeedCacheReadPort feedCacheReadPort,
                          GetCelebrityFolloweesUseCase getCelebrityFolloweesUseCase,
                          GetRecentPostsByAuthorUseCase getRecentPostsByAuthorUseCase,
                          GetPostsByIdsUseCase getPostsByIdsUseCase,
                          GetUserSummaryUseCase getUserSummaryUseCase,
                          CelebrityThresholdProperties celebrityThresholdProperties) {
        this.feedCacheReadPort = feedCacheReadPort;
        this.getCelebrityFolloweesUseCase = getCelebrityFolloweesUseCase;
        this.getRecentPostsByAuthorUseCase = getRecentPostsByAuthorUseCase;
        this.getPostsByIdsUseCase = getPostsByIdsUseCase;
        this.getUserSummaryUseCase = getUserSummaryUseCase;
        this.celebrityThresholdProperties = celebrityThresholdProperties;
    }

    @Override
    public FeedPage get(long userId, Long cursorEpochMillis, int size) {
        List<PushedPost> pushed = feedCacheReadPort.readPushed(userId, cursorEpochMillis, size);
        List<PostView> pushedViews = getPostsByIdsUseCase.list(pushed.stream().map(PushedPost::postId).toList());

        List<Long> celebrityIds = getCelebrityFolloweesUseCase.celebrityFolloweeIds(
                userId, celebrityThresholdProperties.celebrityThreshold());
        List<PostView> celebrityViews = celebrityIds.stream()
                .flatMap(id -> getRecentPostsByAuthorUseCase.recent(id, size).stream())
                // celebrity pull은 "최근 N개"만 가져오므로, 페이지네이션 커서보다 최신인(이미 이전
                // 페이지에서 봤을) 항목은 걸러낸다. 커서보다 오래된 celebrity 과거 이력까지 깊이
                // 페이지네이션하는 것은 이 캐시(최근 100개)의 범위 밖이라 지원하지 않는다 — 학습
                // 프로젝트 범위의 트레이드오프.
                .filter(view -> cursorEpochMillis == null || view.createdAt().toEpochMilli() < cursorEpochMillis)
                .toList();

        // postId 기준 중복 제거: celebrity 판정 시점 차이로 push/pull 양쪽에 같은 포스트가
        // 걸칠 수 있다 (docs §3.1) — push분을 우선한다.
        Map<Long, PostView> merged = new HashMap<>();
        pushedViews.forEach(view -> merged.put(view.id(), view));
        celebrityViews.forEach(view -> merged.putIfAbsent(view.id(), view));

        List<PostView> ordered = merged.values().stream()
                .sorted(Comparator.comparing(PostView::createdAt).reversed())
                .limit(size)
                .toList();

        Map<Long, GetUserSummaryUseCase.UserSummary> authorCache = new HashMap<>();
        List<FeedItem> items = ordered.stream()
                .map(view -> toFeedItem(view, authorCache))
                .toList();

        Long nextCursor = items.size() == size
                ? items.get(items.size() - 1).createdAt().toEpochMilli()
                : null;

        return new FeedPage(items, nextCursor);
    }

    private FeedItem toFeedItem(PostView view, Map<Long, GetUserSummaryUseCase.UserSummary> authorCache) {
        // 같은 작성자의 포스트가 여러 개일 수 있어(특히 celebrity), 요청 하나 안에서는 캐싱해 중복 조회를 줄인다
        GetUserSummaryUseCase.UserSummary summary = authorCache.computeIfAbsent(
                view.authorId(), getUserSummaryUseCase::getSummary);
        AuthorInfo author = new AuthorInfo(summary.id(), summary.username(), summary.displayName());
        return new FeedItem(view.id(), view.content(), view.createdAt(), author, view.likeCount(), view.replyCount());
    }
}
