package com.alejandro.banking_api.service;

import com.alejandro.banking_api.dto.AccountResponse;
import com.alejandro.banking_api.dto.CreateAccountRequest;
import com.alejandro.banking_api.entity.Account;
import com.alejandro.banking_api.entity.AccountType;
import com.alejandro.banking_api.entity.Role;
import com.alejandro.banking_api.entity.User;
import com.alejandro.banking_api.exception.AccountNotFoundException;
import com.alejandro.banking_api.exception.InactiveUserException;
import com.alejandro.banking_api.exception.UnauthorizedAccessException;
import com.alejandro.banking_api.exception.UserNotFoundException;
import com.alejandro.banking_api.repository.AccountRepository;
import com.alejandro.banking_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {
    private static final String USER_EMAIL = "alex@test.com";
    private static final String FULL_NAME = "Alejandro Hernandez";
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 4, 7, 18, 0);

    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountRepository accountRepository;
    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldCreateAccountSuccessfully() {
        User user = buildUser(
                10L, USER_EMAIL, FULL_NAME, true
        );
        CreateAccountRequest request = new CreateAccountRequest(AccountType.CHECKING);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);

        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(10L);
            account.setCreatedAt(CREATED_AT);
            return account;
        });

        AccountResponse response = accountService.createAccount(USER_EMAIL, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.accountNumber()).startsWith("ES");
        assertThat(response.accountNumber()).hasSize(24);
        assertThat(response.accountType()).isEqualTo(AccountType.CHECKING);
        assertThat(response.balance()).isEqualByComparingTo("0.00");
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, atLeastOnce()).existsByAccountNumber(anyString());

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(1)).save(accountCaptor.capture());

        Account savedAccount = accountCaptor.getValue();

        assertThat(savedAccount.getAccountNumber()).startsWith("ES");
        assertThat(savedAccount.getAccountNumber()).hasSize(24);
        assertThat(savedAccount.getAccountType()).isEqualTo(AccountType.CHECKING);
        assertThat(savedAccount.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(savedAccount.isActive()).isTrue();
        assertThat(savedAccount.getUser()).isSameAs(user);
    }
    @Test
    void shouldThrowWhenUserNotFoundCreatingAccount() {
        CreateAccountRequest request = new CreateAccountRequest(AccountType.SAVINGS);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.createAccount(USER_EMAIL, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, never()).save(any(Account.class));
        verify(accountRepository, never()).existsByAccountNumber(anyString());
    }

    @Test
    void shouldThrowWhenUserIsInactiveCreatingAccount() {
        User inactiveUser = buildUser(1L, USER_EMAIL, FULL_NAME, false);

        CreateAccountRequest request = new CreateAccountRequest(AccountType.SAVINGS);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> accountService.createAccount(USER_EMAIL, request))
                .isInstanceOf(InactiveUserException.class)
                .hasMessage("User not active");

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, never()).save(any(Account.class));
        verify(accountRepository, never()).existsByAccountNumber(anyString());
    }

    @Test
    void shouldGetMyAccountsSuccessfully() {
        User user = buildUser(1L, USER_EMAIL, FULL_NAME, true);

        Account checkingAccount = buildAccount(
                10L,
                "ES1111111111111111111111",
                AccountType.CHECKING,
                "1000.00",
                CREATED_AT,
                user
        );

        Account savingsAccount = buildAccount(
                20L,
                "ES2222222222222222222222",
                AccountType.SAVINGS,
                "500.00",
                CREATED_AT.plusDays(1),
                user
        );

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.findByUserId(user.getId())).thenReturn(List.of(checkingAccount, savingsAccount));

        List<AccountResponse> response = accountService.getMyAccounts(USER_EMAIL);

        assertThat(response).hasSize(2);

        assertThat(response)
                .extracting(AccountResponse::accountNumber)
                .containsExactly("ES1111111111111111111111", "ES2222222222222222222222");

        assertThat(response.get(0).accountType()).isEqualTo(AccountType.CHECKING);
        assertThat(response.get(0).balance()).isEqualByComparingTo("1000.00");

        assertThat(response.get(1).accountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(response.get(1).balance()).isEqualByComparingTo("500.00");

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, times(1)).findByUserId(user.getId());
    }

    @Test
    void shouldThrowWhenUserNotFoundGettingMyAccounts() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getMyAccounts(USER_EMAIL))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, never()).findByUserId(anyLong());
    }

    @Test
    void shouldGetMyAccountByIdSuccessfully() {
        User user = buildUser(1L, USER_EMAIL, FULL_NAME, true);

        Account account = buildAccount(
                10L,
                "ES1111111111111111111111",
                AccountType.CHECKING,
                "1000.00",
                CREATED_AT,
                user
        );

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getMyAccountById(USER_EMAIL, 10L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.accountNumber()).isEqualTo("ES1111111111111111111111");
        assertThat(response.accountType()).isEqualTo(AccountType.CHECKING);
        assertThat(response.balance()).isEqualByComparingTo("1000.00");
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, times(1)).findById(10L);
    }

    @Test
    void shouldThrowWhenUserNotFoundGettingAccountById() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getMyAccountById(USER_EMAIL, 10L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, never()).findById(anyLong());
    }

    @Test
    void shouldThrowWhenAccountNotFound() {
        User user = buildUser(1L, USER_EMAIL, FULL_NAME, true);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getMyAccountById(USER_EMAIL, 10L))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account not found");

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, times(1)).findById(10L);
    }

    @Test
    void shouldThrowWhenAccountDoesNotBelongToUser() {
        User authenticatedUser = buildUser(1L, USER_EMAIL, FULL_NAME, true);
        User realAccountOwner = buildUser(2L, "other@test.com", "Other User", true);

        Account account = buildAccount(
                10L,
                "ES1111111111111111111111",
                AccountType.CHECKING,
                "1000.00",
                CREATED_AT,
                realAccountOwner
        );

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(authenticatedUser));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.getMyAccountById(USER_EMAIL, 10L))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessage("User not authorized");

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, times(1)).findById(10L);
    }


    public User buildUser(Long id, String email, String fullName, boolean active) {
            return User.builder()
                    .id(id)
                    .email(email)
                    .fullName(fullName)
                    .password("encoded-password")
                    .active(active)
                    .role(Role.USER)
                    .createdAt(CREATED_AT)
                    .build();
    }
    private Account buildAccount(
            Long id,
            String accountNumber,
            AccountType accountType,
            String balance,
            LocalDateTime createdAt,
            User user
    ) {
        return Account.builder()
                .id(id)
                .accountNumber(accountNumber)
                .accountType(accountType)
                .balance(new BigDecimal(balance))
                .active(true)
                .createdAt(createdAt)
                .user(user)
                .build();
    }
}
