package com.newsfeed.engagement.application.port.out;

import com.newsfeed.engagement.domain.Reply;

import java.time.Instant;
import java.util.List;

public interface ReplyRepositoryPort {

    Reply save(Reply reply);

    /** postId의 답글을 작성시각 역순으로, cursor(있으면 그 시각보다 이전 것만) 페이지 조회. */
    List<Reply> findByPost(long postId, Instant cursor, int size);

    /** 캐시 hit로 얻은 ID들의 콘텐츠를 한 번의 IN 쿼리로 조회한다. */
    List<Reply> findAllByIds(List<Long> replyIds);
}
