package com.newsfeed.common.config;

import com.newsfeed.common.auth.AuthInterceptor;
import com.newsfeed.common.auth.CurrentUserIdArgumentResolver;
import com.newsfeed.common.ratelimit.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;

    public WebConfig(AuthInterceptor authInterceptor,
                     RateLimitInterceptor rateLimitInterceptor,
                     CurrentUserIdArgumentResolver currentUserIdArgumentResolver) {
        this.authInterceptor = authInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.currentUserIdArgumentResolver = currentUserIdArgumentResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 인증 → 처리율 제한 순서 (제한 키가 사용자 ID 기준이므로 인증이 먼저)
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdArgumentResolver);
    }
}
