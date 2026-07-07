package com.newsfeed.fanout.application.service;

import com.newsfeed.fanout.FanoutProperties;
import com.newsfeed.fanout.application.port.in.ProcessPostCreatedUseCase;
import com.newsfeed.fanout.application.port.out.FeedCacheWritePort;
import com.newsfeed.user.application.port.in.GetFollowCountsUseCase;
import com.newsfeed.user.application.port.in.GetFollowerIdsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * 하이브리드 팬아웃의 핵심 로직 (docs/03-detailed-design.md §3.1, §3.3).
 * user 컨텍스트의 좁은 조회 유스케이스(port.in)만 의존한다 — user의 도메인/어댑터에는
 * 접근하지 않는다 (ArchUnit이 강제).
 */
@Service
public class FanoutService implements ProcessPostCreatedUseCase {

    private static final Logger log = LoggerFactory.getLogger(FanoutService.class);

    private final GetFollowCountsUseCase getFollowCountsUseCase;
    private final GetFollowerIdsUseCase getFollowerIdsUseCase;
    private final FeedCacheWritePort feedCacheWritePort;
    private final FanoutProperties properties;

    public FanoutService(GetFollowCountsUseCase getFollowCountsUseCase,
                         GetFollowerIdsUseCase getFollowerIdsUseCase,
                         FeedCacheWritePort feedCacheWritePort,
                         FanoutProperties properties) {
        this.getFollowCountsUseCase = getFollowCountsUseCase;
        this.getFollowerIdsUseCase = getFollowerIdsUseCase;
        this.feedCacheWritePort = feedCacheWritePort;
        this.properties = properties;
    }

    @Override
    public void handle(long postId, long authorId, Instant createdAt) {
        int followerCount = getFollowCountsUseCase.getFollowerCount(authorId);
        if (followerCount > properties.celebrityThreshold()) {
            // celebrity: 팬아웃 생략. 팔로워는 조회 시점에 author-posts 캐시를 pull해서 병합한다 (feed 컨텍스트, 5단계)
            log.debug("celebrity 팬아웃 생략: authorId={}, followerCount={}", authorId, followerCount);
            return;
        }

        long createdAtEpochMillis = createdAt.toEpochMilli();
        Long cursor = null;
        while (true) {
            List<Long> batch = getFollowerIdsUseCase.page(authorId, cursor, properties.followerBatchSize());
            if (batch.isEmpty()) {
                break;
            }
            feedCacheWritePort.pushToFeeds(batch, postId, createdAtEpochMillis);
            if (batch.size() < properties.followerBatchSize()) {
                break; // 마지막 페이지
            }
            cursor = batch.get(batch.size() - 1);
        }
    }
}
