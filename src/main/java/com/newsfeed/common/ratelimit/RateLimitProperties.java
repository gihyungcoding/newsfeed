package com.newsfeed.common.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "newsfeed.rate-limit")
public record RateLimitProperties(int requestsPerSecond) {
}
