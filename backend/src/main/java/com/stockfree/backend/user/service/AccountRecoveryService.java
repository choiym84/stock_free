package com.stockfree.backend.user.service;

import com.stockfree.backend.common.exception.InvalidAccountTokenException;
import com.stockfree.backend.common.exception.InvalidPasswordException;
import com.stockfree.backend.config.SecurityProperties;
import com.stockfree.backend.security.TokenGenerator;
import com.stockfree.backend.user.domain.AccountToken;
import com.stockfree.backend.user.domain.AccountTokenType;
import com.stockfree.backend.user.domain.User;
import com.stockfree.backend.user.domain.UserStatus;
import com.stockfree.backend.user.event.AccountTokenIssuedEvent;
import com.stockfree.backend.user.event.UserAccessChangedEvent;
import com.stockfree.backend.user.repository.AccountTokenRepository;
import com.stockfree.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountRecoveryService {

    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

    private final AccountTokenRepository accountTokenRepository;
    private final UserRepository userRepository;
    private final TokenGenerator tokenGenerator;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public void issueEmailVerification(User user) {
        if (!user.isEmailVerified()) {
            issue(user, AccountTokenType.EMAIL_VERIFICATION,
                    securityProperties.accountTokens().emailVerificationTtl());
        }
    }

    @Transactional
    public void resendEmailVerification(String email) {
        userRepository.findByEmail(User.normalizeEmail(email))
                .filter(user -> user.getStatus() != UserStatus.WITHDRAWN)
                .ifPresent(this::issueEmailVerification);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        Instant now = clock.instant();
        AccountToken token = requireUsable(rawToken, AccountTokenType.EMAIL_VERIFICATION, now);
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(InvalidAccountTokenException::new);
        user.verifyEmail();
        token.consume(now);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(User.normalizeEmail(email))
                .filter(user -> user.getStatus() != UserStatus.WITHDRAWN)
                .ifPresent(user -> issue(
                        user,
                        AccountTokenType.PASSWORD_RESET,
                        securityProperties.accountTokens().passwordResetTtl()
                ));
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        validatePassword(newPassword);
        Instant now = clock.instant();
        AccountToken token = requireUsable(rawToken, AccountTokenType.PASSWORD_RESET, now);
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(InvalidAccountTokenException::new);
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new InvalidAccountTokenException();
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        if (user.getStatus() == UserStatus.LOCKED) {
            user.changeStatus(UserStatus.ACTIVE);
        }
        token.consume(now);
        eventPublisher.publishEvent(new UserAccessChangedEvent(user.getId(), user.getEmail()));
    }

    private void issue(User user, AccountTokenType type, Duration ttl) {
        accountTokenRepository.deleteByUserIdAndType(user.getId(), type);
        accountTokenRepository.flush();
        String rawToken = tokenGenerator.generate();
        accountTokenRepository.save(AccountToken.issue(
                user.getId(),
                type,
                tokenGenerator.hash(rawToken),
                clock.instant().plus(ttl)
        ));
        eventPublisher.publishEvent(new AccountTokenIssuedEvent(user.getEmail(), type, rawToken));
    }

    private AccountToken requireUsable(String rawToken, AccountTokenType type, Instant now) {
        return accountTokenRepository.findByTokenHashAndType(tokenGenerator.hash(rawToken), type)
                .filter(token -> token.isUsableAt(now))
                .orElseThrow(InvalidAccountTokenException::new);
    }

    private void validatePassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new InvalidPasswordException("Password must not exceed 72 bytes");
        }
    }
}
