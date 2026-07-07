package com.newsfeed.common.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JitteredTtlTest {

    @RepeatedTest(20)
    void 지터는_기준값의_비율_범위_안에_머무른다() {
        Duration base = Duration.ofHours(1);
        Duration jittered = JitteredTtl.of(base, 0.1);

        assertThat(jittered.toMillis())
                .isBetween((long) (base.toMillis() * 0.9), (long) (base.toMillis() * 1.1));
    }

    @Test
    void 지터_비율이_0이면_기준값과_같다() {
        Duration base = Duration.ofMinutes(30);
        assertThat(JitteredTtl.of(base, 0)).isEqualTo(base);
    }

    @Test
    void 지터_비율이_범위를_벗어나면_예외() {
        assertThatThrownBy(() -> JitteredTtl.of(Duration.ofMinutes(1), 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JitteredTtl.of(Duration.ofMinutes(1), -0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
