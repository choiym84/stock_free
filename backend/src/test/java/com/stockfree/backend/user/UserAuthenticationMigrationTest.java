package com.stockfree.backend.user;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class UserAuthenticationMigrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17")
    );

    @Test
    void migrateFromV1_withExistingUser_shouldBackfillAndLockLegacyAccount() throws Exception {
        flyway("1").migrate();

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        ); var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO users (email, nickname)
                    VALUES (' Legacy@Example.COM ', ' legacy_user ')
                    """);
        }

        flyway(null).migrate();

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        ); var statement = connection.createStatement(); var result = statement.executeQuery("""
                SELECT email, nickname, password_hash, role, status, version
                FROM users
                """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString("email")).isEqualTo("legacy@example.com");
            assertThat(result.getString("nickname")).isEqualTo("legacy_user");
            assertThat(result.getString("password_hash")).isEqualTo("!PASSWORD_RESET_REQUIRED!");
            assertThat(result.getString("role")).isEqualTo("USER");
            assertThat(result.getString("status")).isEqualTo("LOCKED");
            assertThat(result.getLong("version")).isZero();
        }
    }

    private Flyway flyway(String target) {
        var configuration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration");
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }
}
