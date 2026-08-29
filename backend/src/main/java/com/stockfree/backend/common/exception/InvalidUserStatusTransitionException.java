package com.stockfree.backend.common.exception;

public class InvalidUserStatusTransitionException extends RuntimeException {

    public InvalidUserStatusTransitionException() {
        super("Withdrawn users cannot be reactivated or locked");
    }
}
