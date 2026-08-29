package com.stockfree.backend.config;

import io.lettuce.core.ClientOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

@Profile("redis-session")
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@EnableRedisIndexedHttpSession(redisNamespace = "stock-free:session")
public class RedisSessionConfig {

    private final RedisSessionProperties properties;

    @Bean
    LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration server = new RedisStandaloneConfiguration(
                properties.host(),
                properties.port()
        );
        if (properties.password() != null && !properties.password().isBlank()) {
            server.setPassword(RedisPassword.of(properties.password()));
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder client =
                LettuceClientConfiguration.builder()
                        .clientOptions(ClientOptions.builder().autoReconnect(true).build());
        if (properties.ssl()) {
            client.useSsl();
        }
        return new LettuceConnectionFactory(server, client.build());
    }
}
