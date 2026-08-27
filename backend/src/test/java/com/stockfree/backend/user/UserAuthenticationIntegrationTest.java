package com.stockfree.backend.user;

import com.stockfree.backend.TestcontainersConfiguration;
import com.stockfree.backend.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class UserAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void deleteUsers() {
        userRepository.deleteAll();
    }

    @Test
    void authenticationFlow_withValidRequests_shouldCreateUseAndDestroySession() throws Exception {
        CsrfCredentials csrf = issueCsrf();

        mockMvc.perform(withCsrf(post("/api/v1/auth/register"), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "nickname": "investor_1",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        assertThat(userRepository.findByEmail("user@example.com"))
                .get()
                .extracting(user -> user.getPasswordHash())
                .asString()
                .startsWith("$2");

        MvcResult loginResult = mockMvc.perform(withCsrf(post("/api/v1/auth/login"), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "USER@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/v1/users/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("investor_1"));

        mockMvc.perform(withCsrf(post("/api/v1/auth/logout").session(session), csrf))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void register_withDuplicateEmail_shouldReturn409() throws Exception {
        CsrfCredentials csrf = issueCsrf();
        String request = """
                {
                  "email": "duplicate@example.com",
                  "nickname": "investor_1",
                  "password": "password123"
                }
                """;

        mockMvc.perform(withCsrf(post("/api/v1/auth/register"), csrf)
                        .contentType(APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(withCsrf(post("/api/v1/auth/register"), csrf)
                        .contentType(APPLICATION_JSON)
                        .content(request.replace("investor_1", "investor_2")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void login_withInvalidPassword_shouldReturn401WithoutSession() throws Exception {
        CsrfCredentials csrf = issueCsrf();

        mockMvc.perform(withCsrf(post("/api/v1/auth/register"), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "nickname": "investor_1",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(withCsrf(post("/api/v1/auth/login"), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void csrf_withoutToken_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void csrf_withIssuedCookieAndToken_shouldAllowRequest() throws Exception {
        CsrfCredentials csrf = issueCsrf();

        mockMvc.perform(withCsrf(post("/api/v1/auth/register"), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "csrf@example.com",
                                  "nickname": "csrf_user",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void cors_fromConfiguredFrontend_shouldAllowCredentialedRequest() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    private CsrfCredentials issueCsrf() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.headerName").value("X-XSRF-TOKEN"))
                .andReturn();

        String body = csrfResult.getResponse().getContentAsString();
        String headerName = JsonPath.read(body, "$.data.headerName");
        String token = JsonPath.read(body, "$.data.token");
        Cookie cookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        return new CsrfCredentials(headerName, token, cookie);
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
