package com.alejandro.banking_api.repository;

import com.alejandro.banking_api.entity.Account;
import com.alejandro.banking_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String email);
    List<Account> findByUserId(Long userId);
    List<Account> findByUser(User user);
    boolean existsByAccountNumber(String accountNumber);
}
