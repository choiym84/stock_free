package com.stockfree.backend.common.api;

import java.time.Instant;
import java.util.List;

public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        Instant timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static ApiResponse<Void> error(String code, String message) {
        return error(code, message, List.of());
    }

    public static ApiResponse<Void> error(String code, String message, List<String> details) {
        return new ApiResponse<>(false, null, new ApiError(code, message, details), Instant.now());
    }
}
