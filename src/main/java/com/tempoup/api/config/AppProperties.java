package com.tempoup.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Matching matching) {

    public record Jwt(String secret, long accessTokenTtlMinutes, long refreshTokenTtlDays) {}
    public record Cors(List<String> allowedOrigins) {}
    public record Matching(int maxFeedSize) {}
}
