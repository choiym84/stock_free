package com.stockfree.backend.common.exception;

public class InvalidAccountTokenException extends RuntimeException {

    public InvalidAccountTokenException() {
        super("Token is invalid or expired");
    }
}
