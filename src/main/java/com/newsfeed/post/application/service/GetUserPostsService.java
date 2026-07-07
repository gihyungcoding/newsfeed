package com.newsfeed.post.application.service;

import com.newsfeed.post.application.port.in.GetUserPostsUseCase;
import com.newsfeed.post.application.port.out.PostRepositoryPort;
import com.newsfeed.post.domain.Post;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class GetUserPostsService implements GetUserPostsUseCase {

    private final PostRepositoryPort postRepository;

    public GetUserPostsService(PostRepositoryPort postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public PagedPosts list(long authorId, Long cursorEpochMillis, int size) {
        Instant cursor = cursorEpochMillis == null ? null : Instant.ofEpochMilli(cursorEpochMillis);
        List<Post> items = postRepository.findByAuthor(authorId, cursor, size);
        // 정확히 size개가 채워졌으면 다음 페이지가 있다고 가정한다(단순화된 휴리스틱).
        // 마지막 페이지가 정확히 size개로 끝나는 드문 경우, 다음 호출이 빈 목록을 반환하며 자연 종료된다.
        Long nextCursor = items.size() == size
                ? items.get(items.size() - 1).createdAt().toEpochMilli()
                : null;
        return new PagedPosts(items, nextCursor);
    }
}
