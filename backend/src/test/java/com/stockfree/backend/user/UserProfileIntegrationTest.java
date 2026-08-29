package com.stockfree.backend.user;

import com.jayway.jsonpath.JsonPath;
import com.stockfree.backend.TestcontainersConfiguration;
import com.stockfree.backend.security.AuthenticationEventRepository;
import com.stockfree.backend.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class UserProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthenticationEventRepository authenticationEventRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void deleteUsers() {
        authenticationEventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void updateNickname_withAuthenticatedUser_shouldPersistAndKeepSession() throws Exception {
        CsrfCredentials csrf = issueCsrf();
        MockHttpSession session = registerAndLogin(csrf, "profile@example.com", "profile_user");

        mockMvc.perform(withCsrf(patch("/api/v1/users/me/nickname").session(session), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"nickname":"renamed_user"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("renamed_user"));

        mockMvc.perform(get("/api/v1/users/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("renamed_user"));
    }

    @Test
    void changePassword_withValidCurrentPassword_shouldRevokeSessionAndReplacePassword() throws Exception {
        CsrfCredentials csrf = issueCsrf();
        MockHttpSession session = registerAndLogin(csrf, "password@example.com", "password_user");

        mockMvc.perform(withCsrf(put("/api/v1/users/me/password").session(session), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword":"password123",
                                  "newPassword":"new-password456"
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("SESSION_EXPIRED"));

        login(csrf, "password@example.com", "password123")
                .andExpect(status().isUnauthorized());
        login(csrf, "password@example.com", "new-password456")
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_withWrongCurrentPassword_shouldRejectAndKeepSession() throws Exception {
        CsrfCredentials csrf = issueCsrf();
        MockHttpSession session = registerAndLogin(csrf, "wrong@example.com", "wrong_user");

        mockMvc.perform(withCsrf(put("/api/v1/users/me/password").session(session), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword":"wrong-password",
                                  "newPassword":"new-password456"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_CURRENT_PASSWORD"));

        mockMvc.perform(get("/api/v1/users/me").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void withdraw_withValidPassword_shouldRevokeSessionAndDisableLogin() throws Exception {
        CsrfCredentials csrf = issueCsrf();
        MockHttpSession session = registerAndLogin(csrf, "withdraw@example.com", "withdraw_user");

        mockMvc.perform(withCsrf(delete("/api/v1/users/me").session(session), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"password123"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("SESSION_EXPIRED"));
        login(csrf, "withdraw@example.com", "password123")
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession registerAndLogin(
            CsrfCredentials csrf,
            String email,
            String nickname
    ) throws Exception {
        mockMvc.perform(withCsrf(post("/api/v1/auth/register"), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"%s",
                                  "nickname":"%s",
                                  "password":"password123"
                                }
                                """.formatted(email, nickname)))
                .andExpect(status().isCreated());
        MvcResult result = login(csrf, email, "password123")
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private org.springframework.test.web.servlet.ResultActions login(
            CsrfCredentials csrf,
            String email,
            String password
    ) throws Exception {
        return mockMvc.perform(withCsrf(post("/api/v1/auth/login"), csrf)
                .contentType(APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password)));
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
