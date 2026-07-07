package com.newsfeed.post.adapter.out.kafka;

import com.newsfeed.common.messaging.Topics;
import com.newsfeed.post.domain.event.PostCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 트랜잭션 커밋 후 post-created 이벤트를 Kafka로 발행한다 (팬아웃 트리거).
 *
 * <p>이 발행이 실패하면(브로커 장애, 프로세스 종료 등) 해당 포스트는 어떤 팔로워에게도
 * 팬아웃되지 않는다 — 지금은 학습 프로젝트 규모에서 이 위험을 감수하고, 실무 승격
 * 기준은 docs/05-msa-roadmap.md의 "Transactional Outbox 패턴 승격 기준"에 기록해 두었다.
 */
@Component
class PostCreatedKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(PostCreatedKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    PostCreatedKafkaPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener
    void on(PostCreatedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(
                    new PostCreatedMessage(event.postId(), event.authorId(), event.createdAt().toEpochMilli()));
            // 키를 authorId로 잡아 같은 작성자의 이벤트가 같은 파티션에 순서대로 쌓이게 한다
            kafkaTemplate.send(Topics.POST_CREATED, String.valueOf(event.authorId()), payload);
        } catch (JacksonException e) {
            log.error("post-created 이벤트 직렬화 실패: postId={}", event.postId(), e);
        }
    }
}
