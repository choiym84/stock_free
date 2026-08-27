package com.stockfree.backend.user.controller;

import com.stockfree.backend.common.api.ApiResponse;
import com.stockfree.backend.security.AuthenticatedUser;
import com.stockfree.backend.user.dto.UserResponse;
import com.stockfree.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ApiResponse.ok(UserResponse.from(userService.getById(authenticatedUser.id())));
    }
}
