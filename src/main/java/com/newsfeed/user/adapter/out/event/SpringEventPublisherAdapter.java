package com.newsfeed.user.adapter.out.event;

import com.newsfeed.user.application.port.out.UserEventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 프로세스 내부 이벤트 발행 어댑터. MSA 분리 시 이 어댑터만 Kafka 발행으로 교체하면 된다.
 */
@Component
class SpringEventPublisherAdapter implements UserEventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    SpringEventPublisherAdapter(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);
    }
}
