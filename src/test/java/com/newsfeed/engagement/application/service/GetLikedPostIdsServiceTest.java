package com.newsfeed.engagement.application.service;

import com.newsfeed.engagement.application.port.out.LikeRepositoryPort;
import com.newsfeed.engagement.application.port.out.PostLikersCachePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GetLikedPostIdsServiceTest {

    @Mock
    PostLikersCachePort postLikersCache;
    @Mock
    LikeRepositoryPort likeRepository;

    @InjectMocks
    GetLikedPostIdsService service;

    @Test
    void 캐시가_있으면_캐시로_판정하고_DB는_건드리지_않는다() {
        given(postLikersCache.exists(100L)).willReturn(true);
        given(postLikersCache.isMember(100L, 1L)).willReturn(true);

        Set<Long> result = service.likedPostIds(List.of(100L), 1L);

        assertThat(result).containsExactly(100L);
        verify(likeRepository, never()).exists(100L, 1L);
    }

    @Test
    void 캐시가_콜드면_DB로_판정한다() {
        given(postLikersCache.exists(200L)).willReturn(false);
        given(likeRepository.exists(200L, 1L)).willReturn(true);

        Set<Long> result = service.likedPostIds(List.of(200L), 1L);

        assertThat(result).containsExactly(200L);
    }

    @Test
    void 좋아요하지_않은_postId는_결과에서_빠진다() {
        given(postLikersCache.exists(100L)).willReturn(true);
        given(postLikersCache.isMember(100L, 1L)).willReturn(false);

        Set<Long> result = service.likedPostIds(List.of(100L), 1L);

        assertThat(result).isEmpty();
    }
}
