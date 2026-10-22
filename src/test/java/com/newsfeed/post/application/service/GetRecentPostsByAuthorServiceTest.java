package com.newsfeed.post.application.service;

import com.newsfeed.post.application.port.in.GetPostsByIdsUseCase;
import com.newsfeed.post.application.port.in.GetPostsByIdsUseCase.PostView;
import com.newsfeed.post.application.port.out.AuthorPostsCachePort;
import com.newsfeed.post.application.port.out.PostCachePort;
import com.newsfeed.post.application.port.out.PostRepositoryPort;
import com.newsfeed.post.domain.Post;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** celebrity pull의 두 경로 — author-posts 캐시 hit / 콜드 스타트 DB 재구성+캐시 재적재. */
@ExtendWith(MockitoExtension.class)
class GetRecentPostsByAuthorServiceTest {

    @Mock
    AuthorPostsCachePort authorPostsCache;
    @Mock
    PostCachePort postCache;
    @Mock
    PostRepositoryPort postRepository;
    @Mock
    GetPostsByIdsUseCase getPostsByIdsUseCase;

    @InjectMocks
    GetRecentPostsByAuthorService service;

    private PostView view(long id) {
        return new PostView(id, 9L, "content-" + id, 0, 0, Instant.ofEpochMilli(1000L * id));
    }

    @Test
    void author_posts_캐시가_있으면_DB를_조회하지_않는다() {
        given(authorPostsCache.recentPostIds(9L, 20)).willReturn(List.of(2L, 1L));
        given(getPostsByIdsUseCase.list(List.of(2L, 1L))).willReturn(List.of(view(2L), view(1L)));

        List<PostView> result = service.recent(9L, 20);

        assertThat(result).extracting(PostView::id).containsExactly(2L, 1L);
        verify(postRepository, never()).findByAuthor(anyLong(), org.mockito.ArgumentMatchers.any(), anyInt());
    }

    @Test
    void 콜드_스타트면_DB에서_재구성하고_두_캐시를_다시_채운다() {
        Post p1 = new Post(1L, 9L, "content-1", Instant.ofEpochMilli(1000));
        Post p2 = new Post(2L, 9L, "content-2", Instant.ofEpochMilli(2000));
        given(authorPostsCache.recentPostIds(9L, 20)).willReturn(List.of());
        given(postRepository.findByAuthor(9L, null, 20)).willReturn(List.of(p2, p1));
        given(getPostsByIdsUseCase.list(List.of(2L, 1L))).willReturn(List.of(view(2L), view(1L)));

        List<PostView> result = service.recent(9L, 20);

        assertThat(result).hasSize(2);
        // 자가 치유: author-posts 캐시와 post 콘텐츠 캐시를 모두 되채워, 다음 요청부터 캐시 히트가 되게 한다
        verify(authorPostsCache).push(9L, 1L, 1000L);
        verify(authorPostsCache).push(9L, 2L, 2000L);
        verify(postCache).save(p1);
        verify(postCache).save(p2);
    }
}
