package com.newsfeed.engagement.application.port.out;

/** 도메인 이벤트 발행 포트. user/post 컨텍스트의 동명 포트와 같은 패턴. */
public interface EngagementEventPublisherPort {

    void publish(Object event);
}
