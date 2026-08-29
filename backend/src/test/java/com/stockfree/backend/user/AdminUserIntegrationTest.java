package com.stockfree.backend.user;

import com.jayway.jsonpath.JsonPath;
import com.stockfree.backend.TestcontainersConfiguration;
import com.stockfree.backend.security.AuthenticationEventRepository;
import com.stockfree.backend.user.domain.UserRole;
import com.stockfree.backend.user.repository.UserRepository;
import com.stockfree.backend.user.service.UserService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.security.require-email-verification=false")
@AutoConfigureMockMvc
class AdminUserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationEventRepository authenticationEventRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    void deleteUsers() {
        authenticationEventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void changeRole_asAdmin_shouldUpdateRoleAndRevokeTargetSession() throws Exception {
        CsrfCredentials csrf = issueCsrf();
        register(csrf, "admin@example.com", "admin_user");
        Long adminId = userRepository.findByEmail("admin@example.com").orElseThrow().getId();
        userService.changeRole(adminId, UserRole.ADMIN);
        MockHttpSession adminSession = login(csrf, "admin@example.com");

        mockMvc.perform(get("/api/v1/admin/authentication-events").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].attemptedEmail").value("admin@example.com"));

        register(csrf, "target@example.com", "target_user");
        Long targetId = userRepository.findByEmail("target@example.com").orElseThrow().getId();
        MockHttpSession targetSession = login(csrf, "target@example.com");

        mockMvc.perform(withCsrf(patch("/api/v1/admin/users/{userId}/role", targetId)
                        .session(adminSession), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"role":"ADMIN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        mockMvc.perform(get("/api/v1/users/me").session(targetSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("SESSION_EXPIRED"));
    }

    @Test
    void getAll_asNormalUser_shouldReturn403() throws Exception {
        CsrfCredentials csrf = issueCsrf();
        register(csrf, "user@example.com", "normal_user");
        MockHttpSession userSession = login(csrf, "user@example.com");

        mockMvc.perform(get("/api/v1/admin/users").session(userSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void changeOwnStatus_asAdmin_shouldReturn409() throws Exception {
        CsrfCredentials csrf = issueCsrf();
        register(csrf, "admin@example.com", "admin_user");
        Long adminId = userRepository.findByEmail("admin@example.com").orElseThrow().getId();
        userService.changeRole(adminId, UserRole.ADMIN);
        MockHttpSession adminSession = login(csrf, "admin@example.com");

        mockMvc.perform(withCsrf(patch("/api/v1/admin/users/{userId}/status", adminId)
                        .session(adminSession), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"status":"LOCKED"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SELF_ACCESS_CHANGE_NOT_ALLOWED"));
    }

    private void register(CsrfCredentials csrf, String email, String nickname) throws Exception {
        mockMvc.perform(withCsrf(post("/api/v1/auth/register"), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"%s","nickname":"%s","password":"password123"}
                                """.formatted(email, nickname)))
                .andExpect(status().isCreated());
    }

    private MockHttpSession login(CsrfCredentials csrf, String email) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(post("/api/v1/auth/login"), csrf)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
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
