package com.newsfeed.post.adapter.out.event;

import com.newsfeed.post.application.port.out.PostEventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
class PostSpringEventPublisherAdapter implements PostEventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    PostSpringEventPublisherAdapter(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);
    }
}
