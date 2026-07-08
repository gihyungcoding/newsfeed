package com.newsfeed.post.application.port.out;

import com.newsfeed.post.domain.PostCounts;

import java.util.Optional;

/** 횟수 캐시 (cnt:post:{id}, Redis Hash). user 컨텍스트의 UserCountCachePort와 같은 패턴. */
public interface PostCountCachePort {

    Optional<PostCounts> find(long postId);

    void save(long postId, PostCounts counts);

    /** 캐시 키가 있을 때만 증분한다. 키가 없으면 다음 조회 miss 때 DB 값으로 재적재되어 정합성이 복구된다. */
    void incrementIfPresent(long postId, int likeDelta, int replyDelta);
}
