/**
 * fanout 컨텍스트 — post-created 이벤트를 소비해 팔로워들의 피드 캐시에 전파하는 팬아웃 워커.
 * 하이브리드 전략: 팔로워 수가 임계값을 넘는 작성자(celebrity)는 팬아웃을 생략한다.
 */
package com.newsfeed.fanout;
