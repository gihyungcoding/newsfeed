package com.newsfeed.engagement.application.service;

import com.newsfeed.engagement.application.port.in.GetRepliesUseCase;
import com.newsfeed.engagement.application.port.out.PostRepliesCachePort;
import com.newsfeed.engagement.application.port.out.ReplyRepositoryPort;
import com.newsfeed.engagement.domain.Reply;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * post 컨텍스트의 GetRecentPostsByAuthorService와 같은 하이브리드 구조 —
 * 첫 페이지(cursor 없음)는 캐시 우선, 그보다 깊은 페이지네이션이나 콜드 스타트는 DB로.
 */
@Service
public class GetRepliesService implements GetRepliesUseCase {

    private final PostRepliesCachePort postRepliesCache;
    private final ReplyRepositoryPort replyRepository;

    public GetRepliesService(PostRepliesCachePort postRepliesCache, ReplyRepositoryPort replyRepository) {
        this.postRepliesCache = postRepliesCache;
        this.replyRepository = replyRepository;
    }

    @Override
    public PagedReplies list(long postId, Long cursorEpochMillis, int size) {
        if (cursorEpochMillis == null) {
            List<Long> cachedIds = postRepliesCache.recentReplyIds(postId, size);
            if (!cachedIds.isEmpty()) {
                List<Reply> replies = replyRepository.findAllByIds(cachedIds).stream()
                        .sorted(Comparator.comparing(Reply::createdAt).reversed())
                        .toList();
                return new PagedReplies(replies, nextCursor(replies, size));
            }
        }

        Instant cursor = cursorEpochMillis == null ? null : Instant.ofEpochMilli(cursorEpochMillis);
        List<Reply> items = replyRepository.findByPost(postId, cursor, size);
        if (cursorEpochMillis == null) {
            // 콜드 스타트: 캐시를 다시 채워 다음 조회부터는 캐시 히트가 되게 한다
            items.forEach(r -> postRepliesCache.push(postId, r.id(), r.createdAt().toEpochMilli()));
        }
        return new PagedReplies(items, nextCursor(items, size));
    }

    private Long nextCursor(List<Reply> items, int size) {
        return items.size() == size ? items.get(items.size() - 1).createdAt().toEpochMilli() : null;
    }
}
