package com.stockfree.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.notification")
public record NotificationProperties(
        String baseUrl,
        String from
) {
}
