package com.stockfree.backend.common.exception;

public class DuplicateUserAttributeException extends RuntimeException {

    private final String code;

    public DuplicateUserAttributeException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
