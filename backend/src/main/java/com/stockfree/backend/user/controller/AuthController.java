package com.stockfree.backend.user.controller;

import com.stockfree.backend.common.api.ApiResponse;
import com.stockfree.backend.security.AuthenticatedUser;
import com.stockfree.backend.security.SessionAuthenticationHandler;
import com.stockfree.backend.user.dto.CsrfTokenResponse;
import com.stockfree.backend.user.dto.LoginRequest;
import com.stockfree.backend.user.dto.RegisterUserRequest;
import com.stockfree.backend.user.dto.UserResponse;
import com.stockfree.backend.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final SessionAuthenticationHandler sessionAuthenticationHandler;

    @GetMapping("/csrf")
    public ApiResponse<CsrfTokenResponse> csrf(CsrfToken csrfToken) {
        return ApiResponse.ok(CsrfTokenResponse.from(csrfToken));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterUserRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(UserResponse.from(userService.register(request))));
    }

    @PostMapping("/login")
    public ApiResponse<UserResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Authentication authentication = userService.authenticate(loginRequest);
        sessionAuthenticationHandler.login(authentication, request, response);
        return ApiResponse.ok(UserResponse.from((AuthenticatedUser) authentication.getPrincipal()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        sessionAuthenticationHandler.logout(authentication, request, response);
        return ResponseEntity.noContent().build();
    }
}
