package com.newsfeed.common.messaging;

/**
 * Kafka 토픽 이름 상수. post(발행)와 fanout(구독) 컨텍스트가 서로의 클래스를 참조하지
 * 않고도 같은 토픽 이름을 공유하도록 공용 인프라(common)에 둔다.
 * 이 문자열이 곧 두 컨텍스트가 나중에 별도 서비스로 분리됐을 때 남는 유일한 계약이다
 * — 메시지 스키마({@code PostCreatedMessage})는 각 컨텍스트가 독립적으로 정의한다.
 */
public final class Topics {

    private Topics() {
    }

    public static final String POST_CREATED = "post-created";
}
