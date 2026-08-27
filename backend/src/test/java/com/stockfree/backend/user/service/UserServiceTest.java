package com.stockfree.backend.user.service;

import com.stockfree.backend.common.exception.DuplicateUserAttributeException;
import com.stockfree.backend.user.domain.User;
import com.stockfree.backend.user.dto.RegisterUserRequest;
import com.stockfree.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    }

    @Test
    void register_withExistingEmail_shouldThrowConflictException() {
        var request = new RegisterUserRequest("user@example.com", "investor_1", "password123");
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateUserAttributeException.class)
                .hasMessage("Email is already registered");
    }
}
