package com.stockfree.backend.common.exception;

public class SelfAccessChangeNotAllowedException extends RuntimeException {

    public SelfAccessChangeNotAllowedException() {
        super("Administrators cannot change their own role or status");
    }
}
