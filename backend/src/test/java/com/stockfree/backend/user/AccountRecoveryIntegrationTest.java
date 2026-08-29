package com.stockfree.backend.user;

import com.jayway.jsonpath.JsonPath;
import com.stockfree.backend.TestcontainersConfiguration;
import com.stockfree.backend.security.AuthenticationEventRepository;
import com.stockfree.backend.security.TokenGenerator;
import com.stockfree.backend.user.repository.AccountTokenRepository;
import com.stockfree.backend.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AccountRecoveryIntegrationTest {

    private static final String VERIFICATION_TOKEN = "verification-token";
    private static final String RESET_TOKEN = "reset-token";
    private static final String VERIFICATION_HASH = "a".repeat(64);
    private static final String RESET_HASH = "b".repeat(64);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountTokenRepository accountTokenRepository;

    @Autowired
    private AuthenticationEventRepository authenticationEventRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private TokenGenerator tokenGenerator;

    @BeforeEach
    void setUp() {
        accountTokenRepository.deleteAll();
        authenticationEventRepository.deleteAll();
        userRepository.deleteAll();
        when(tokenGenerator.hash(VERIFICATION_TOKEN)).thenReturn(VERIFICATION_HASH);
        when(tokenGenerator.hash(RESET_TOKEN)).thenReturn(RESET_HASH);
    }

    @Test
    void emailVerification_thenPasswordReset_shouldEnforceOneTimeTokens() throws Exception {
        CsrfCredentials csrf = issueCsrf();
        when(tokenGenerator.generate()).thenReturn(VERIFICATION_TOKEN, RESET_TOKEN);

        mockMvc.perform(withCsrf(post("/api/v1/auth/register"), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"recovery@example.com",
                                  "nickname":"recovery_user",
                                  "password":"password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.emailVerified").value(false));

        login(csrf, "password123").andExpect(status().isUnauthorized());

        mockMvc.perform(withCsrf(post("/api/v1/auth/email-verifications/confirm"), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"token":"verification-token"}
                                """))
                .andExpect(status().isNoContent());
        login(csrf, "password123").andExpect(status().isOk());

        mockMvc.perform(withCsrf(post("/api/v1/auth/password-resets"), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"recovery@example.com"}
                                """))
                .andExpect(status().isAccepted());
        mockMvc.perform(withCsrf(post("/api/v1/auth/password-resets/confirm"), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"token":"reset-token","newPassword":"new-password456"}
                                """))
                .andExpect(status().isNoContent());

        login(csrf, "password123").andExpect(status().isUnauthorized());
        login(csrf, "new-password456").andExpect(status().isOk());

        mockMvc.perform(withCsrf(post("/api/v1/auth/password-resets/confirm"), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"token":"reset-token","newPassword":"another-password789"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_OR_EXPIRED_TOKEN"));
    }

    @Test
    void requestPasswordReset_withUnknownEmail_shouldReturnAcceptedWithoutToken() throws Exception {
        CsrfCredentials csrf = issueCsrf();

        mockMvc.perform(withCsrf(post("/api/v1/auth/password-resets"), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"unknown@example.com"}
                                """))
                .andExpect(status().isAccepted());

        assertThat(accountTokenRepository.count()).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions login(
            CsrfCredentials csrf,
            String password
    ) throws Exception {
        return mockMvc.perform(withCsrf(post("/api/v1/auth/login"), csrf)
                .contentType(APPLICATION_JSON)
                .content("""
                        {"email":"recovery@example.com","password":"%s"}
                        """.formatted(password)));
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

    private MockHttpServletRequestBuilder withCsrf(
            MockHttpServletRequestBuilder request,
            CsrfCredentials csrf
    ) {
        return request.cookie(csrf.cookie()).header(csrf.headerName(), csrf.token());
    }

    private record CsrfCredentials(String headerName, String token, Cookie cookie) {
    }
}
