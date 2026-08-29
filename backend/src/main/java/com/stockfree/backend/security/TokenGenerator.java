package com.stockfree.backend.security;

public interface TokenGenerator {

    String generate();

    String hash(String token);
}
