package com.newsfeed.fanout.adapter.in.messaging;

import com.newsfeed.common.messaging.Topics;
import com.newsfeed.fanout.application.port.in.ProcessPostCreatedUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

/**
 * 팬아웃 워커의 진입점. 역직렬화 실패나 유스케이스 예외가 던져지면 Spring Kafka의 기본
 * 오류 처리기가 재시도 후 로그를 남기고 넘어간다(at-least-once, 무한 재시도는 아님).
 * 실제 팬아웃 로직은 {@link ProcessPostCreatedUseCase}에 위임해, 이 클래스는 역직렬화만 한다.
 */
@Component
class PostCreatedKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(PostCreatedKafkaListener.class);

    private final ProcessPostCreatedUseCase useCase;
    private final ObjectMapper objectMapper;

    PostCreatedKafkaListener(ProcessPostCreatedUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.POST_CREATED, groupId = "${spring.kafka.consumer.group-id}")
    void onMessage(String payload) {
        PostCreatedMessage message = objectMapper.readValue(payload, PostCreatedMessage.class);
        log.debug("post-created 수신: postId={}, authorId={}", message.postId(), message.authorId());
        useCase.handle(message.postId(), message.authorId(), Instant.ofEpochMilli(message.createdAt()));
    }
}
