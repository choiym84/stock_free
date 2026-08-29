package com.stockfree.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthenticationHistoryService {

    private final AuthenticationEventRepository authenticationEventRepository;

    public Page<AuthenticationEvent> getForEmail(String email, Pageable pageable) {
        return authenticationEventRepository.findByAttemptedEmailOrderByCreatedAtDesc(email, pageable);
    }

    public Page<AuthenticationEvent> getAll(Pageable pageable) {
        return authenticationEventRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
