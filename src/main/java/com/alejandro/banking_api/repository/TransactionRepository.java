package com.alejandro.banking_api.repository;

import com.alejandro.banking_api.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    //devuelve todas las transacciones de una cuenta(por el id de la cuenta)
    Page<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);
    //search all transactions of user
    List<Transaction> findByAccountUserIdOrderByCreatedAtDesc(Long userId);
}
