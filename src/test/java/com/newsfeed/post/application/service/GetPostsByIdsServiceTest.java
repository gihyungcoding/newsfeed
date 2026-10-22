package com.newsfeed.post.application.service;

import com.newsfeed.post.application.port.in.GetPostsByIdsUseCase.PostView;
import com.newsfeed.post.application.port.out.PostCachePort;
import com.newsfeed.post.application.port.out.PostCountCachePort;
import com.newsfeed.post.application.port.out.PostRepositoryPort;
import com.newsfeed.post.domain.Post;
import com.newsfeed.post.domain.PostCounts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** 콘텐츠·횟수 각각의 look-aside(캐시 우선, miss만 DB)가 올바르게 조립되는지 검증한다. */
@ExtendWith(MockitoExtension.class)
class GetPostsByIdsServiceTest {

    @Mock
    PostCachePort postCache;
    @Mock
    PostRepositoryPort postRepository;
    @Mock
    PostCountCachePort postCountCache;

    @InjectMocks
    GetPostsByIdsService service;

    private Post post(long id) {
        return new Post(id, 9L, "content-" + id, Instant.ofEpochMilli(1000L * id));
    }

    @Test
    void 콘텐츠와_횟수가_모두_캐시에_있으면_DB를_조회하지_않는다() {
        given(postCache.find(1L)).willReturn(Optional.of(post(1L)));
        given(postCountCache.find(1L)).willReturn(Optional.of(new PostCounts(3, 2)));

        List<PostView> result = service.list(List.of(1L));

        assertThat(result).containsExactly(new PostView(1L, 9L, "content-1", 3, 2, Instant.ofEpochMilli(1000)));
        verify(postRepository, never()).findAllByIds(anyList());
        verify(postRepository, never()).findCounts(anyLong());
    }

    @Test
    void 콘텐츠_캐시_miss만_모아_한_번의_IN_조회로_가져오고_캐시에_채운다() {
        Post fromDb = post(2L);
        given(postCache.find(1L)).willReturn(Optional.of(post(1L)));
        given(postCache.find(2L)).willReturn(Optional.empty());
        given(postRepository.findAllByIds(List.of(2L))).willReturn(List.of(fromDb));
        given(postCountCache.find(anyLong())).willReturn(Optional.of(PostCounts.zero()));

        List<PostView> result = service.list(List.of(1L, 2L));

        assertThat(result).extracting(PostView::id).containsExactly(1L, 2L); // 입력 순서 유지
        verify(postRepository, times(1)).findAllByIds(List.of(2L));
        verify(postCache).save(fromDb);
    }

    @Test
    void 횟수_캐시가_miss면_DB_컬럼값으로_채운다() {
        given(postCache.find(1L)).willReturn(Optional.of(post(1L)));
        given(postCountCache.find(1L)).willReturn(Optional.empty());
        given(postRepository.findCounts(1L)).willReturn(Optional.of(new PostCounts(5, 1)));

        List<PostView> result = service.list(List.of(1L));

        assertThat(result.get(0).likeCount()).isEqualTo(5);
        verify(postCountCache).save(1L, new PostCounts(5, 1));
    }

    @Test
    void 존재하지_않는_postId는_결과에서_빠지고_횟수_캐시를_오염시키지_않는다() {
        given(postCache.find(404L)).willReturn(Optional.empty());
        given(postRepository.findAllByIds(List.of(404L))).willReturn(List.of());

        List<PostView> result = service.list(List.of(404L));

        assertThat(result).isEmpty();
        verify(postCountCache, never()).find(anyLong());
        verify(postCountCache, never()).save(anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 빈_입력이면_아무것도_조회하지_않는다() {
        assertThat(service.list(List.of())).isEmpty();
        verify(postCache, never()).find(anyLong());
    }
}
