package com.stockfree.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("app.security")
public record SecurityProperties(List<String> allowedOrigins) {

    public SecurityProperties {
        allowedOrigins = List.copyOf(allowedOrigins);
    }
}
