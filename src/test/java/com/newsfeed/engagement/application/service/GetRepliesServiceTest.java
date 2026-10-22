package com.newsfeed.engagement.application.service;

import com.newsfeed.engagement.application.port.in.GetRepliesUseCase.PagedReplies;
import com.newsfeed.engagement.application.port.out.PostRepliesCachePort;
import com.newsfeed.engagement.application.port.out.ReplyRepositoryPort;
import com.newsfeed.engagement.domain.Reply;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** 첫 페이지는 캐시 우선, 깊은 페이지네이션/콜드 스타트는 DB — 세 경로를 각각 검증한다. */
@ExtendWith(MockitoExtension.class)
class GetRepliesServiceTest {

    @Mock
    PostRepliesCachePort postRepliesCache;
    @Mock
    ReplyRepositoryPort replyRepository;

    @InjectMocks
    GetRepliesService service;

    private Reply reply(long id, long createdAtMillis) {
        return new Reply(id, 100L, 2L, "reply-" + id, Instant.ofEpochMilli(createdAtMillis));
    }

    @Test
    void 첫_페이지에_캐시가_있으면_DB_목록_조회_없이_작성시각_역순으로_반환한다() {
        given(postRepliesCache.recentReplyIds(100L, 20)).willReturn(List.of(1L, 2L));
        // findAllById는 순서를 보장하지 않으므로 일부러 뒤섞인 순서로 돌려준다
        given(replyRepository.findAllByIds(List.of(1L, 2L))).willReturn(List.of(reply(1L, 1000), reply(2L, 2000)));

        PagedReplies result = service.list(100L, null, 20);

        assertThat(result.items()).extracting(Reply::id).containsExactly(2L, 1L);
        verify(replyRepository, never()).findByPost(anyLong(), any(), anyInt());
    }

    @Test
    void 콜드_스타트면_DB에서_조회하고_캐시를_다시_채운다() {
        given(postRepliesCache.recentReplyIds(100L, 20)).willReturn(List.of());
        given(replyRepository.findByPost(100L, null, 20)).willReturn(List.of(reply(2L, 2000), reply(1L, 1000)));

        PagedReplies result = service.list(100L, null, 20);

        assertThat(result.items()).hasSize(2);
        verify(postRepliesCache).push(100L, 1L, 1000L);
        verify(postRepliesCache).push(100L, 2L, 2000L);
    }

    @Test
    void 커서가_있으면_캐시를_보지_않고_DB로_깊은_페이지를_조회한다() {
        given(replyRepository.findByPost(100L, Instant.ofEpochMilli(5000), 20)).willReturn(List.of(reply(1L, 1000)));

        PagedReplies result = service.list(100L, 5000L, 20);

        assertThat(result.items()).extracting(Reply::id).containsExactly(1L);
        verify(postRepliesCache, never()).recentReplyIds(anyLong(), anyInt());
        verify(postRepliesCache, never()).push(anyLong(), anyLong(), anyLong()); // 깊은 페이지는 캐시에 적재하지 않는다
    }

    @Test
    void 결과가_size와_같으면_nextCursor를_채운다() {
        given(postRepliesCache.recentReplyIds(100L, 1)).willReturn(List.of());
        given(replyRepository.findByPost(100L, null, 1)).willReturn(List.of(reply(1L, 1000)));

        assertThat(service.list(100L, null, 1).nextCursor()).isEqualTo(1000L);
    }
}
