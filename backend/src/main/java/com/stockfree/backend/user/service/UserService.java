package com.stockfree.backend.user.service;

import com.stockfree.backend.common.exception.DuplicateUserAttributeException;
import com.stockfree.backend.common.exception.InvalidPasswordException;
import com.stockfree.backend.common.exception.UserNotFoundException;
import com.stockfree.backend.user.domain.User;
import com.stockfree.backend.user.dto.LoginRequest;
import com.stockfree.backend.user.dto.RegisterUserRequest;
import com.stockfree.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public User register(RegisterUserRequest request) {
        String email = User.normalizeEmail(request.email());
        String nickname = request.nickname().trim();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateUserAttributeException("EMAIL_ALREADY_EXISTS", "Email is already registered");
        }
        if (userRepository.existsByNickname(nickname)) {
            throw new DuplicateUserAttributeException("NICKNAME_ALREADY_EXISTS", "Nickname is already registered");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new InvalidPasswordException("Password must not exceed 72 bytes");
        }

        User user = User.register(email, nickname, passwordEncoder.encode(request.password()));
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateUserAttributeException(
                    "USER_ALREADY_EXISTS",
                    "Email or nickname is already registered"
            );
        }
    }

    public Authentication authenticate(LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new BadCredentialsException("Invalid credentials");
        }
        var authenticationToken = UsernamePasswordAuthenticationToken.unauthenticated(
                User.normalizeEmail(request.email()),
                request.password()
        );
        return authenticationManager.authenticate(authenticationToken);
    }

    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    }
}
