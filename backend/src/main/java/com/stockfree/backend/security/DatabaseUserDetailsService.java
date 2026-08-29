package com.stockfree.backend.security;

import com.stockfree.backend.config.SecurityProperties;
import com.stockfree.backend.user.domain.User;
import com.stockfree.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final SecurityProperties securityProperties;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return userRepository.findByEmail(User.normalizeEmail(email))
                .map(user -> AuthenticatedUser.from(user, securityProperties.requireEmailVerification()))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
}
