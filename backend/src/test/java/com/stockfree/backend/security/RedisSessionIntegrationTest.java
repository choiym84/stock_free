package com.stockfree.backend.security;

import com.jayway.jsonpath.JsonPath;
import com.stockfree.backend.TestcontainersConfiguration;
import com.stockfree.backend.user.domain.UserRole;
import com.stockfree.backend.user.repository.AccountTokenRepository;
import com.stockfree.backend.user.repository.UserRepository;
import com.stockfree.backend.user.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("redis-session")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.security.require-email-verification=false")
@AutoConfigureMockMvc
class RedisSessionIntegrationTest {

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:8-alpine")
    ).withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("app.redis-session.host", REDIS::getHost);
        registry.add("app.redis-session.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisIndexedSessionRepository sessionRepository;

    @Autowired
    private AccountTokenRepository accountTokenRepository;

    @Autowired
    private AuthenticationEventRepository authenticationEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    void deleteUsers() {
        accountTokenRepository.deleteAll();
        authenticationEventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void accessChange_withRedisSession_shouldDeleteIndexedSession() throws Exception {
        CsrfCredentials csrf = issueCsrf();
        mockMvc.perform(post("/api/v1/auth/register")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"redis@example.com",
                                  "nickname":"redis_user",
                                  "password":"password123"
                                }
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"redis@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk());

        assertThat(sessionRepository.findByPrincipalName("redis@example.com")).hasSize(1);
        Long userId = userRepository.findByEmail("redis@example.com").orElseThrow().getId();

        userService.changeRole(userId, UserRole.ADMIN);

        assertThat(sessionRepository.findByPrincipalName("redis@example.com")).isEmpty();
    }

    private CsrfCredentials issueCsrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        return new CsrfCredentials(
                JsonPath.read(body, "$.data.headerName"),
                JsonPath.read(body, "$.data.token"),
                cookie
        );
    }

    private record CsrfCredentials(String headerName, String token, Cookie cookie) {
    }
}
