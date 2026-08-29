package com.stockfree.backend.common.exception;

import com.stockfree.backend.common.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toDetail)
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDATION_FAILED", "Invalid request", details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableRequest() {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_REQUEST_BODY", "Request body is missing or malformed"));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPassword(InvalidPasswordException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_PASSWORD", exception.getMessage()));
    }

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCurrentPassword(
            InvalidCurrentPasswordException exception
    ) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_CURRENT_PASSWORD", exception.getMessage()));
    }

    @ExceptionHandler(InvalidAccountTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidAccountToken(InvalidAccountTokenException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_OR_EXPIRED_TOKEN", exception.getMessage()));
    }

    @ExceptionHandler(DuplicateUserAttributeException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateUserAttribute(
            DuplicateUserAttributeException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationFailure() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("INVALID_CREDENTIALS", "Email or password is incorrect"));
    }

    @ExceptionHandler(LoginRateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleLoginRateLimit(LoginRateLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", Long.toString(exception.getRetryAfter().toSeconds()))
                .body(ApiResponse.error("LOGIN_RATE_LIMITED", exception.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("USER_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(SelfAccessChangeNotAllowedException.class)
    public ResponseEntity<ApiResponse<Void>> handleSelfAccessChange(SelfAccessChangeNotAllowedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("SELF_ACCESS_CHANGE_NOT_ALLOWED", exception.getMessage()));
    }

    @ExceptionHandler(InvalidUserStatusTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStatusTransition(
            InvalidUserStatusTransitionException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("INVALID_USER_STATUS_TRANSITION", exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private String toDetail(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
