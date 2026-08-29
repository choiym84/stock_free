package com.stockfree.backend.user.controller;

import com.stockfree.backend.common.api.ApiResponse;
import com.stockfree.backend.common.api.PageResponse;
import com.stockfree.backend.security.AuthenticationHistoryService;
import com.stockfree.backend.user.dto.AdminAuthenticationEventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/authentication-events")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAuthenticationEventController {

    private final AuthenticationHistoryService authenticationHistoryService;

    @GetMapping
    public ApiResponse<PageResponse<AdminAuthenticationEventResponse>> getAll(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(
                authenticationHistoryService.getAll(pageable).map(AdminAuthenticationEventResponse::from)
        ));
    }
}
