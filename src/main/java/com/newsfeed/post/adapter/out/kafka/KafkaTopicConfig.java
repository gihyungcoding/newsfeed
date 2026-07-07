package com.newsfeed.post.adapter.out.kafka;

import com.newsfeed.common.messaging.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * post-created 토픽을 프로듀서(post 컨텍스트) 쪽에서 정의한다 — 토픽 소유권이 발행자에게 있다는
 * 관례. 파티션 3개로 만들어 authorId 키 기반 파티셔닝(§3.3)이 실제로 여러 파티션에 분산되게 한다.
 */
@Configuration
class KafkaTopicConfig {

    @Bean
    NewTopic postCreatedTopic() {
        return TopicBuilder.name(Topics.POST_CREATED).partitions(3).replicas(1).build();
    }
}
