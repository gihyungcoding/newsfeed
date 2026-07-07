package com.newsfeed.fanout.application.service;

import com.newsfeed.fanout.FanoutProperties;
import com.newsfeed.fanout.application.port.out.FeedCacheWritePort;
import com.newsfeed.user.application.port.in.GetFollowCountsUseCase;
import com.newsfeed.user.application.port.in.GetFollowerIdsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 하이브리드 팬아웃의 핵심 분기(celebrity 임계값, 배치 페이지네이션)를
 * 실제 Kafka/Redis 없이 포트 모킹만으로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class FanoutServiceTest {

    @Mock
    GetFollowCountsUseCase getFollowCountsUseCase;

    @Mock
    GetFollowerIdsUseCase getFollowerIdsUseCase;

    @Mock
    FeedCacheWritePort feedCacheWritePort;

    private FanoutService fanoutService(int celebrityThreshold, int batchSize) {
        return new FanoutService(getFollowCountsUseCase, getFollowerIdsUseCase, feedCacheWritePort,
                new FanoutProperties(celebrityThreshold, batchSize, 500));
    }

    @Test
    void 팔로워_수가_임계값_이하면_팬아웃한다() {
        given(getFollowCountsUseCase.getFollowerCount(1L)).willReturn(3);
        given(getFollowerIdsUseCase.page(1L, null, 10)).willReturn(List.of(10L, 20L, 30L));

        fanoutService(1000, 10).handle(100L, 1L, Instant.ofEpochMilli(5000));

        verify(feedCacheWritePort).pushToFeeds(List.of(10L, 20L, 30L), 100L, 5000L);
    }

    @Test
    void celebrity면_팬아웃을_생략한다() {
        given(getFollowCountsUseCase.getFollowerCount(1L)).willReturn(1001);

        fanoutService(1000, 10).handle(100L, 1L, Instant.ofEpochMilli(5000));

        verify(feedCacheWritePort, never()).pushToFeeds(anyList(), anyLong(), anyLong());
        verify(getFollowerIdsUseCase, never()).page(anyLong(), org.mockito.ArgumentMatchers.any(), anyInt());
    }

    @Test
    void 팔로워가_배치_크기보다_많으면_여러_페이지를_순회한다() {
        given(getFollowCountsUseCase.getFollowerCount(1L)).willReturn(5);
        // 배치 크기 2: 첫 페이지 가득 참(더 있음을 암시) → 두 번째 페이지 일부만(마지막 페이지)
        given(getFollowerIdsUseCase.page(1L, null, 2)).willReturn(List.of(10L, 20L));
        given(getFollowerIdsUseCase.page(1L, 20L, 2)).willReturn(List.of(30L));

        fanoutService(1000, 2).handle(100L, 1L, Instant.ofEpochMilli(5000));

        verify(feedCacheWritePort).pushToFeeds(List.of(10L, 20L), 100L, 5000L);
        verify(feedCacheWritePort).pushToFeeds(List.of(30L), 100L, 5000L);
        verify(getFollowerIdsUseCase, never()).page(1L, 30L, 2);
    }
}
