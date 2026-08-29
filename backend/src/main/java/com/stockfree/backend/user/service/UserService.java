package com.stockfree.backend.user.service;

import com.stockfree.backend.common.exception.DuplicateUserAttributeException;
import com.stockfree.backend.common.exception.InvalidPasswordException;
import com.stockfree.backend.common.exception.UserNotFoundException;
import com.stockfree.backend.user.domain.User;
import com.stockfree.backend.user.domain.UserRole;
import com.stockfree.backend.user.domain.UserStatus;
import com.stockfree.backend.user.dto.LoginRequest;
import com.stockfree.backend.user.dto.RegisterUserRequest;
import com.stockfree.backend.user.event.UserAccessChangedEvent;
import com.stockfree.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

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
            throw translateRegistrationConflict(exception);
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

    @Transactional
    public User changeRole(Long id, UserRole role) {
        User user = getById(id);
        if (user.changeRole(role)) {
            eventPublisher.publishEvent(new UserAccessChangedEvent(id));
        }
        return user;
    }

    @Transactional
    public User changeStatus(Long id, UserStatus status) {
        User user = getById(id);
        if (user.changeStatus(status)) {
            eventPublisher.publishEvent(new UserAccessChangedEvent(id));
        }
        return user;
    }

    private RuntimeException translateRegistrationConflict(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                return switch (constraintViolation.getConstraintName()) {
                    case "uk_users_email" -> new DuplicateUserAttributeException(
                            "EMAIL_ALREADY_EXISTS",
                            "Email is already registered"
                    );
                    case "uk_users_nickname" -> new DuplicateUserAttributeException(
                            "NICKNAME_ALREADY_EXISTS",
                            "Nickname is already registered"
                    );
                    default -> exception;
                };
            }
            cause = cause.getCause();
        }
        return exception;
    }
}
