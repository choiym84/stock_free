package com.stockfree.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.redis-session")
public record RedisSessionProperties(
        String host,
        int port,
        String password,
        boolean ssl
) {
}
