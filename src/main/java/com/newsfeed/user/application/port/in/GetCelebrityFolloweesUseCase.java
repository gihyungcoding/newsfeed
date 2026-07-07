package com.newsfeed.user.application.port.in;

import java.util.List;

/**
 * 특정 사용자가 팔로우하는 대상 중 팔로워 수가 threshold를 넘는(celebrity) 사람들의 ID를 조회한다.
 * feed 컨텍스트가 피드 조회 시 "이 사람들의 포스트는 pull로 병합해야 한다"를 판단하는 데 쓴다
 * (docs/03-detailed-design.md §3.1, §3.4).
 *
 * <p>threshold를 호출자가 넘기게 한 이유: celebrity 판정 기준(newsfeed.fanout.celebrity-threshold)은
 * fanout(쓰기 경로)과 feed(읽기 경로) 양쪽이 반드시 같은 값을 써야 하는 정책이다. user 컨텍스트가
 * 그 설정을 소유하게 하는 대신, 호출자가 원시값으로 넘기게 해 user는 정책의 값 자체에는
 * 무관심하도록 뒀다.
 */
public interface GetCelebrityFolloweesUseCase {

    List<Long> celebrityFolloweeIds(long followerId, int celebrityThreshold);
}
