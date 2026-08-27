package com.stockfree.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionAuthenticationHandler {

    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;

    public void login(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);
    }

    public void logout(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        csrfTokenRepository.saveToken(null, request, response);
    }
}
