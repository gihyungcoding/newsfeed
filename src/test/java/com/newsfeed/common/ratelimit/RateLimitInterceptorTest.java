package com.newsfeed.common.ratelimit;

import com.newsfeed.common.auth.AuthInterceptor;
import com.newsfeed.common.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitInterceptorTest {

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    @Mock
    HttpServletRequest request;

    @Test
    void 한도_이내면_통과한다() {
        RateLimitInterceptor interceptor = interceptor(limit(3), count(3L));

        assertThat(interceptor.preHandle(request, null, null)).isTrue();
    }

    @Test
    void 한도를_넘으면_429_오류가_난다() {
        RateLimitInterceptor interceptor = interceptor(limit(3), count(4L));

        assertThatThrownBy(() -> interceptor.preHandle(request, null, null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void 비인증_요청은_제한하지_않는다() {
        given(request.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE)).willReturn(null);
        RateLimitInterceptor interceptor = new RateLimitInterceptor(redisTemplate, limit(1));

        assertThat(interceptor.preHandle(request, null, null)).isTrue();
    }

    private RateLimitInterceptor interceptor(RateLimitProperties props, long currentCount) {
        given(request.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE)).willReturn(1L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(anyString())).willReturn(currentCount);
        return new RateLimitInterceptor(redisTemplate, props);
    }

    private RateLimitProperties limit(int requestsPerSecond) {
        return new RateLimitProperties(requestsPerSecond);
    }

    private long count(long count) {
        return count;
    }
}
