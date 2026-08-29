package com.stockfree.backend.user.service;

import com.stockfree.backend.common.exception.DuplicateUserAttributeException;
import com.stockfree.backend.user.domain.User;
import com.stockfree.backend.user.domain.UserRole;
import com.stockfree.backend.user.dto.RegisterUserRequest;
import com.stockfree.backend.user.event.UserAccessChangedEvent;
import com.stockfree.backend.user.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AccountRecoveryService accountRecoveryService;

    @InjectMocks
    private UserService userService;

    @Test
    void register_withValidRequest_shouldNormalizeEmailAndSavePasswordHash() {
        var request = new RegisterUserRequest(" User@Example.COM ", "investor_1", "password123");
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register(request);

        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getNickname()).isEqualTo("investor_1");
        assertThat(result.getPasswordHash()).isEqualTo("encoded-password");
        verify(userRepository).saveAndFlush(any(User.class));
        verify(accountRecoveryService).issueEmailVerification(result);
    }

    @Test
    void register_withExistingEmail_shouldThrowConflictException() {
        var request = new RegisterUserRequest("user@example.com", "investor_1", "password123");
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateUserAttributeException.class)
                .hasMessage("Email is already registered");
    }

    @Test
    void register_whenDatabaseReportsEmailConstraint_shouldReturnEmailConflict() {
        var request = new RegisterUserRequest("user@example.com", "investor_1", "password123");
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(dataIntegrityViolation("uk_users_email"));

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateUserAttributeException.class)
                .extracting("code")
                .isEqualTo("EMAIL_ALREADY_EXISTS");
    }

    @Test
    void register_whenDatabaseReportsNicknameConstraint_shouldReturnNicknameConflict() {
        var request = new RegisterUserRequest("user@example.com", "investor_1", "password123");
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(dataIntegrityViolation("uk_users_nickname"));

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateUserAttributeException.class)
                .extracting("code")
                .isEqualTo("NICKNAME_ALREADY_EXISTS");
    }

    @Test
    void register_whenDatabaseReportsUnknownConstraint_shouldPreserveDatabaseFailure() {
        var request = new RegisterUserRequest("user@example.com", "investor_1", "password123");
        DataIntegrityViolationException failure = dataIntegrityViolation("ck_users_email_not_blank");
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(failure);

        assertThatThrownBy(() -> userService.register(request)).isSameAs(failure);
    }

    @Test
    void changeRole_withDifferentRole_shouldChangeUserAndPublishEvent() {
        User user = User.register("user@example.com", "investor_1", "encoded-password");
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));

        User result = userService.changeRole(1L, UserRole.ADMIN);

        assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);
        verify(eventPublisher).publishEvent(new UserAccessChangedEvent(1L, "user@example.com"));
    }

    @Test
    void changeRole_withSameRole_shouldNotPublishEvent() {
        User user = User.register("user@example.com", "investor_1", "encoded-password");
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));

        userService.changeRole(1L, UserRole.USER);

        verify(eventPublisher, never()).publishEvent(any());
    }

    private DataIntegrityViolationException dataIntegrityViolation(String constraintName) {
        var cause = new ConstraintViolationException(
                "constraint violation",
                new SQLException("constraint violation"),
                constraintName
        );
        return new DataIntegrityViolationException("database failure", cause);
    }
}
