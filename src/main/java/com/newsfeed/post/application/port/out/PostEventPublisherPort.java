package com.newsfeed.post.application.port.out;

/** 도메인 이벤트 발행 포트. user 컨텍스트의 UserEventPublisherPort와 같은 패턴. */
public interface PostEventPublisherPort {

    void publish(Object event);
}
