package com.stockfree.backend.common.api;

import java.util.List;

public record ApiError(String code, String message, List<String> details) {
}
