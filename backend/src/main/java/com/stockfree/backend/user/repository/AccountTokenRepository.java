package com.stockfree.backend.user.repository;

import com.stockfree.backend.user.domain.AccountToken;
import com.stockfree.backend.user.domain.AccountTokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountTokenRepository extends JpaRepository<AccountToken, Long> {

    Optional<AccountToken> findByTokenHashAndType(String tokenHash, AccountTokenType type);

    void deleteByUserIdAndType(Long userId, AccountTokenType type);
}
