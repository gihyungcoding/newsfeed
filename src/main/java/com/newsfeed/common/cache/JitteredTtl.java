package com.newsfeed.common.cache;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 같은 시각에 대량 적재된 캐시 키들이 한꺼번에 만료되면, 그 순간 DB로 조회가 몰리는
 * "캐시 눈사태(cache avalanche)"가 발생한다. TTL에 소량의 무작위 지터를 더해
 * 만료 시점을 흩뿌려 눈사태를 예방한다.
 */
public final class JitteredTtl {

    private JitteredTtl() {
    }

    /**
     * baseTtl에 ±jitterRatio 만큼의 무작위 편차를 더한다.
     * 예: of(Duration.ofHours(1), 0.1) → 54분~66분 사이의 무작위 TTL
     */
    public static Duration of(Duration baseTtl, double jitterRatio) {
        if (jitterRatio < 0 || jitterRatio >= 1) {
            throw new IllegalArgumentException("jitterRatio는 [0, 1) 범위여야 합니다: " + jitterRatio);
        }
        long baseMillis = baseTtl.toMillis();
        long jitterRange = (long) (baseMillis * jitterRatio);
        long delta = jitterRange == 0 ? 0 : ThreadLocalRandom.current().nextLong(-jitterRange, jitterRange + 1);
        return Duration.ofMillis(baseMillis + delta);
    }
}
