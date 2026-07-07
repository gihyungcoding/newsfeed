package com.newsfeed.user.application.port.out;

/**
 * 도메인 이벤트 발행 아웃바운드 포트.
 * 지금은 프로세스 내부(Spring 이벤트)로 구현되지만, 포트 뒤에 숨겨져 있으므로
 * MSA 분리 시 Kafka 발행으로 교체해도 application 계층은 바뀌지 않는다.
 */
public interface UserEventPublisherPort {

    void publish(Object event);
}
