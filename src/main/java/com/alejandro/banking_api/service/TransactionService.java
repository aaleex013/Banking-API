package com.alejandro.banking_api.service;

import com.alejandro.banking_api.dto.TransactionResponse;
import com.alejandro.banking_api.entity.Account;
import com.alejandro.banking_api.entity.Transaction;
import com.alejandro.banking_api.entity.User;
import com.alejandro.banking_api.exception.AccountNotFoundException;
import com.alejandro.banking_api.exception.UnauthorizedAccessException;
import com.alejandro.banking_api.exception.UserNotFoundException;
import com.alejandro.banking_api.repository.AccountRepository;
import com.alejandro.banking_api.repository.TransactionRepository;
import com.alejandro.banking_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public List<TransactionResponse> getTransactionsByAccountId(String email, Long accountId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new UserNotFoundException("User not found"));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(()-> new AccountNotFoundException("Account not found"));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("You are not allowed to perform this operation");
        }
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId, Pageable.unpaged())
                .getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
