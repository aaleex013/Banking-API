package com.alejandro.banking_api.service;

import com.alejandro.banking_api.dto.TransactionResponse;
import com.alejandro.banking_api.entity.*;
import com.alejandro.banking_api.exception.AccountNotFoundException;
import com.alejandro.banking_api.exception.UnauthorizedAccessException;
import com.alejandro.banking_api.exception.UserNotFoundException;
import com.alejandro.banking_api.repository.AccountRepository;
import com.alejandro.banking_api.repository.TransactionRepository;
import com.alejandro.banking_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {
    private static final String USER_EMAIL = "alex@test.com";
    private static final String ACCOUNT_NUMBER = "ES1111111111111111111111";
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 4, 7, 12, 35);

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void shouldGetTransactionsByAccountIdSuccessfully() {
        User user = buildUser(1L, USER_EMAIL, "Alejandro Hernandez");

        Account account = buildAccount(
                user
        );

        Transaction debitTransaction = buildTransaction(
                100L,
                TransactionType.DEBIT,
                "250.00",
                "Transferencia enviada",
                CREATED_AT,
                account
        );

        Transaction creditTransaction = buildTransaction(
                101L,
                TransactionType.CREDIT,
                "500.00",
                "Ingreso recibido",
                CREATED_AT.plusMinutes(5),
                account
        );

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccountIdOrderByCreatedAtDesc(10L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(creditTransaction, debitTransaction)));

        List<TransactionResponse> response = transactionService.getTransactionsByAccountId(USER_EMAIL, 10L);

        assertThat(response).hasSize(2);

        assertThat(response.getFirst().id()).isEqualTo(101L);
        assertThat(response.getFirst().type()).isEqualTo(TransactionType.CREDIT);
        assertThat(response.get(0).amount()).isEqualByComparingTo("500.00");
        assertThat(response.get(0).createdAt()).isEqualTo(CREATED_AT.plusMinutes(5));

        assertThat(response.get(1).id()).isEqualTo(100L);
        assertThat(response.get(1).type()).isEqualTo(TransactionType.DEBIT);
        assertThat(response.get(1).amount()).isEqualByComparingTo("250.00");
        assertThat(response.get(1).createdAt()).isEqualTo(CREATED_AT);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, times(1)).findById(10L);
        verify(transactionRepository, times(1))
                .findByAccountIdOrderByCreatedAtDesc(10L, Pageable.unpaged());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionsByAccountId(USER_EMAIL, 10L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, never()).findById(anyLong());
        verify(transactionRepository, never())
                .findByAccountIdOrderByCreatedAtDesc(anyLong(), any(Pageable.class));
    }

    @Test
    void shouldThrowWhenAccountNotFound() {
        User user = buildUser(1L, USER_EMAIL, "Alejandro Hernandez");

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionsByAccountId(USER_EMAIL, 10L))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account not found");

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, times(1)).findById(10L);
        verify(transactionRepository, never())
                .findByAccountIdOrderByCreatedAtDesc(anyLong(), any(Pageable.class));
    }

    @Test
    void shouldThrowWhenAccountDoesNotBelongToUser() {
        User authenticatedUser = buildUser(1L, USER_EMAIL, "Alejandro Hernandez");
        User realAccountOwner = buildUser(2L, "other@test.com", "Other User");

        Account account = buildAccount(
                realAccountOwner
        );

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(authenticatedUser));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.getTransactionsByAccountId(USER_EMAIL, 10L))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessage("You are not allowed to perform this operation");

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, times(1)).findById(10L);
        verify(transactionRepository, never())
                .findByAccountIdOrderByCreatedAtDesc(anyLong(), any(Pageable.class));
    }

    private User buildUser(Long id, String email, String fullName) {
        return User.builder()
                .id(id)
                .email(email)
                .fullName(fullName)
                .password("encoded-password")
                .role(Role.USER)
                .active(true)
                .build();
    }

    private Account buildAccount(
            User user
    ) {
        return Account.builder()
                .id(10L)
                .accountNumber(TransactionServiceTest.ACCOUNT_NUMBER)
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("750.00"))
                .active(true)
                .user(user)
                .build();
    }

    private Transaction buildTransaction(
            Long id,
            TransactionType type,
            String amount,
            String description,
            LocalDateTime createdAt,
            Account account
    ) {
        return Transaction.builder()
                .id(id)
                .type(type)
                .amount(new BigDecimal(amount))
                .description(description)
                .createdAt(createdAt)
                .account(account)
                .build();
    }

}
