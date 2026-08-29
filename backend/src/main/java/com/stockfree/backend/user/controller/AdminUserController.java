package com.stockfree.backend.user.controller;

import com.stockfree.backend.common.api.ApiResponse;
import com.stockfree.backend.common.api.PageResponse;
import com.stockfree.backend.security.AuthenticatedUser;
import com.stockfree.backend.user.dto.AdminUserResponse;
import com.stockfree.backend.user.dto.ChangeUserRoleRequest;
import com.stockfree.backend.user.dto.ChangeUserStatusRequest;
import com.stockfree.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> getAll(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(userService.getAll(pageable).map(AdminUserResponse::from)));
    }

    @PatchMapping("/{userId}/role")
    public ApiResponse<AdminUserResponse> changeRole(
            @AuthenticationPrincipal AuthenticatedUser administrator,
            @PathVariable Long userId,
            @Valid @RequestBody ChangeUserRoleRequest request
    ) {
        return ApiResponse.ok(AdminUserResponse.from(
                userService.changeRoleByAdmin(administrator.id(), userId, request.role())
        ));
    }

    @PatchMapping("/{userId}/status")
    public ApiResponse<AdminUserResponse> changeStatus(
            @AuthenticationPrincipal AuthenticatedUser administrator,
            @PathVariable Long userId,
            @Valid @RequestBody ChangeUserStatusRequest request
    ) {
        return ApiResponse.ok(AdminUserResponse.from(
                userService.changeStatusByAdmin(administrator.id(), userId, request.status())
        ));
    }
}
