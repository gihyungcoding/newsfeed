package com.newsfeed.common.ratelimit;

import com.newsfeed.common.auth.AuthInterceptor;
import com.newsfeed.common.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * 책의 "웹 서버 - 처리율 제한" 역할. Redis 카운터 기반 고정 윈도(fixed window) 방식.
 * 사용자별로 초 단위 윈도 키를 만들어 INCR하고, 한도를 넘으면 429를 반환한다.
 *
 * <p>고정 윈도는 윈도 경계에서 순간적으로 한도의 2배까지 허용될 수 있는 단순한 방식이다.
 * 토큰 버킷 등 정교한 알고리즘(책 4장)은 로드맵으로 남긴다.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    public RateLimitInterceptor(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object userId = request.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        if (userId == null) {
            return true; // 비인증(공개) 요청은 제한하지 않는다
        }
        long window = System.currentTimeMillis() / 1000;
        String key = "ratelimit:%s:%d".formatted(userId, window);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 윈도가 지나면 키가 자연 소멸하도록 여유를 둔 TTL
            redisTemplate.expire(key, Duration.ofSeconds(2));
        }
        if (count != null && count > properties.requestsPerSecond()) {
            throw ApiException.tooManyRequests("RATE_LIMIT_EXCEEDED",
                    "초당 요청 한도(%d)를 초과했습니다".formatted(properties.requestsPerSecond()));
        }
        return true;
    }
}
