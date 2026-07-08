package com.newsfeed.engagement.adapter.out.event;

import com.newsfeed.engagement.application.port.out.EngagementEventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 클래스 이름을 컨텍스트 접두어로 구분한다 — user/post 컨텍스트에 같은 이름의 어댑터를
 * SpringEventPublisherAdapter로 뒀다가 스캔 시 빈 이름 충돌(ConflictingBeanDefinitionException)이
 * 난 적이 있다 (3→4단계 사이 실제로 겪은 문제).
 */
@Component
class EngagementSpringEventPublisherAdapter implements EngagementEventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    EngagementSpringEventPublisherAdapter(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);
    }
}
