package com.stockfree.backend.security;

public record AuthenticationClient(String ipAddress, String userAgent) {

    private static final int MAX_IP_ADDRESS_LENGTH = 45;
    private static final int MAX_USER_AGENT_LENGTH = 255;

    public AuthenticationClient {
        ipAddress = truncate(ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress,
                MAX_IP_ADDRESS_LENGTH);
        userAgent = userAgent == null || userAgent.isBlank()
                ? null
                : truncate(userAgent, MAX_USER_AGENT_LENGTH);
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
