package com.newsfeed.common.auth;

import com.newsfeed.common.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 책의 "웹 서버 - 인증" 역할을 단순화한 것.
 * X-USER-ID 헤더를 신뢰하는 방식이며, 실제 서비스라면 JWT/세션 검증으로 교체한다.
 * 인증 로직이 이 인터셉터 한 곳에 격리되어 있어 교체 비용이 작다. (docs/03-detailed-design.md §3.6)
 *
 * <p>사용자 존재 여부는 여기서 확인하지 않는다(DB 조회 없이 통과).
 * 존재 검증이 필요한 유스케이스(팔로우 등)가 직접 검증한다.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_HEADER = "X-USER-ID";
    public static final String USER_ID_ATTRIBUTE = "authenticatedUserId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isPublic(request)) {
            return true;
        }
        String header = request.getHeader(USER_ID_HEADER);
        if (header == null || header.isBlank()) {
            throw ApiException.unauthorized("UNAUTHORIZED", "X-USER-ID 헤더가 필요합니다");
        }
        try {
            request.setAttribute(USER_ID_ATTRIBUTE, Long.parseLong(header));
        } catch (NumberFormatException e) {
            throw ApiException.unauthorized("UNAUTHORIZED", "X-USER-ID 헤더가 올바르지 않습니다: " + header);
        }
        return true;
    }

    /** 사용자 생성만 인증 없이 허용한다 (생성해야 ID가 생기므로). */
    private boolean isPublic(HttpServletRequest request) {
        return "POST".equals(request.getMethod()) && "/api/users".equals(request.getRequestURI());
    }
}
