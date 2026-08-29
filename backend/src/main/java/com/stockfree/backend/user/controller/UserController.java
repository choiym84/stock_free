package com.stockfree.backend.user.controller;

import com.stockfree.backend.common.api.ApiResponse;
import com.stockfree.backend.security.AuthenticatedUser;
import com.stockfree.backend.user.dto.UserResponse;
import com.stockfree.backend.user.dto.UpdateNicknameRequest;
import com.stockfree.backend.user.dto.ChangePasswordRequest;
import com.stockfree.backend.user.dto.WithdrawUserRequest;
import com.stockfree.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PatchMapping("/me/nickname")
    public ApiResponse<UserResponse> updateNickname(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateNicknameRequest request
    ) {
        return ApiResponse.ok(UserResponse.from(userService.updateNickname(authenticatedUser.id(), request)));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(authenticatedUser.id(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody WithdrawUserRequest request
    ) {
        userService.withdraw(authenticatedUser.id(), request);
        return ResponseEntity.noContent().build();
    }
}
