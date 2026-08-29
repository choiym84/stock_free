package com.stockfree.backend.common.exception;

public class InvalidCurrentPasswordException extends RuntimeException {

    public InvalidCurrentPasswordException() {
        super("Current password is incorrect");
    }
}
