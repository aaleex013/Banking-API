package com.alejandro.banking_api.repository;

import com.alejandro.banking_api.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String email);
    List<Account> findByUserId(Long userId);
    boolean existsByAccountNumber(String accountNumber);
}
