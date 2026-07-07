package com.newsfeed.user.application.port.in;

import java.util.List;

/**
 * 특정 사용자의 팔로워 ID를 페이지 단위로 조회한다.
 * 팬아웃 워커가 팔로워 5,000명을 한 번에 메모리에 올리지 않기 위해 사용한다.
 * (docs/03-detailed-design.md §3.3)
 */
public interface GetFollowerIdsUseCase {

    /**
     * @param afterFollowerId 이 ID보다 큰 팔로워부터 반환 (null이면 처음부터)
     * @return followerId 오름차순 목록. 비어 있으면 더 이상 페이지가 없다는 뜻
     */
    List<Long> page(long followeeId, Long afterFollowerId, int limit);
}
