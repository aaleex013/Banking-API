package com.alejandro.banking_api.service;

import com.alejandro.banking_api.dto.CreateTransferRequest;
import com.alejandro.banking_api.dto.TransferResponse;
import com.alejandro.banking_api.entity.*;
import com.alejandro.banking_api.exception.*;
import com.alejandro.banking_api.repository.AccountRepository;
import com.alejandro.banking_api.repository.TransactionRepository;
import com.alejandro.banking_api.repository.TransferRepository;
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

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {
    private static final String USER_EMAIL = "alex@test.com";
    private static final String SOURCE_ACCOUNT_NUMBER = "ES11111111111111111111";
    private static final String DESTINATION_ACCOUNT_NUMBER = "ES22222222222222222222";
    private static final BigDecimal AMOUNT = new BigDecimal("250.00");
    private static final LocalDateTime CREATED_AT = LocalDateTime.now();

    @Mock
    private TransferRepository transferRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private TransferService transferService;

    /*camino feliz de crear la transferencia
    verifica user y cuenta existen, cuenta activa
    cuenta origen pertenece usuario, saldo suficiente
    transferencia creada correctamente, se descuenta dinero de la cuenta origen
    suma dinero a la cuenta destino, se guarda la transferencia
    se crean transacciones(DEBIT, CREDIT)
    */

    @Test
    public void shouldCreateTransferSuccessfully() {
        User sourceUser = buildUser(1L, USER_EMAIL, "Alejandro Hernandez");
        User destinationUser = buildUser(2L, "lucia@test.com", "Lucia Hernandez");

        Account sourceAccount = buildAccount(
                10L,
                SOURCE_ACCOUNT_NUMBER,
                AccountType.CHECKING,
                "1000.00",
                true,
                sourceUser
        );
        Account destinationAccount = buildAccount(
                10L,
                DESTINATION_ACCOUNT_NUMBER,
                AccountType.SAVINGS,
                "500.00",
                true,
                destinationUser
        );


        CreateTransferRequest request = buildValidRequest();

        Transfer savedTransfer = Transfer.builder()
                .id(100L)
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .amount(AMOUNT)
                .description("Prueba transferencia correctamente")
                .status(TransferStatus.COMPLETED)
                .createdAt(CREATED_AT)
                .build();
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(sourceUser));
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT_NUMBER)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber(DESTINATION_ACCOUNT_NUMBER)).thenReturn(Optional.of(destinationAccount));
        when(transferRepository.save(any(Transfer.class))).thenReturn(savedTransfer);

        TransferResponse response = transferService.createTransfer(USER_EMAIL, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.sourceAccountNumber()).isEqualTo(SOURCE_ACCOUNT_NUMBER);
        assertThat(response.destinationAccountNumber()).isEqualTo(DESTINATION_ACCOUNT_NUMBER);
        assertThat(response.sourceFullName()).isEqualTo("Alejandro Hernandez");
        assertThat(response.amount()).isEqualByComparingTo("250.00");
        assertThat(response.description()).isEqualTo("Prueba transferencia correctamente");
        assertThat(response.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);

        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("750.00");
        assertThat(destinationAccount.getBalance()).isEqualByComparingTo("750.00");

        ArgumentCaptor<Transfer> transferCaptor = ArgumentCaptor.forClass(Transfer.class);
        verify(transferRepository, times(1)).save(transferCaptor.capture());

        Transfer transferToSave = transferCaptor.getValue();

        assertThat(transferToSave.getSourceAccount()).isSameAs(sourceAccount);
        assertThat(transferToSave.getDestinationAccount()).isSameAs(destinationAccount);
        assertThat(transferToSave.getAmount()).isEqualByComparingTo("250.00");
        assertThat(transferToSave.getDescription()).isEqualTo("Prueba transferencia correctamente");
        assertThat(transferToSave.getStatus()).isEqualTo(TransferStatus.COMPLETED);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());

        List<Transaction> transactions = transactionCaptor.getAllValues();

        assertThat(transactions).hasSize(2);

        assertThat(transactions)
                .anySatisfy(transaction -> {
                    assertThat(transaction.getAmount()).isEqualByComparingTo("250.00");
                    assertThat(transaction.getType()).isEqualTo(TransactionType.DEBIT);
                    assertThat(transaction.getAccount()).isSameAs(sourceAccount);
                    assertThat(transaction.getTransfer()).isSameAs(savedTransfer);
                });
        assertThat(transactions)
                .anySatisfy(transaction -> {
                    assertThat(transaction.getAmount()).isEqualByComparingTo("250.00");
                    assertThat(transaction.getType()).isEqualTo(TransactionType.CREDIT);
                    assertThat(transaction.getAccount()).isSameAs(destinationAccount);
                    assertThat(transaction.getTransfer()).isSameAs(savedTransfer);
                });

    }

    /*
    Comprueba que no se puede crear una transferencia si el usuario autenticado no existe
     */
    @Test
    void shouldThrowWhenUserNotFound() {
        CreateTransferRequest request = buildValidRequest();
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.createTransfer(USER_EMAIL, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(accountRepository, never()).findByAccountNumber(anyString());
        verifyNoTransferOrTransactionWasSaved();
    }

    /*
    Comprueba que no se puede crear transferencia si la cuenta origen no existe
     */
    @Test
    void shouldThrowWhenAccountSourceNotFound() {
        User user = buildUser(1L, USER_EMAIL, "Alejandro Hernandez");
        CreateTransferRequest request = buildValidRequest();
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT_NUMBER)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> transferService.createTransfer(USER_EMAIL, request))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Source account number not found");
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, times(1)).findByAccountNumber(SOURCE_ACCOUNT_NUMBER);
        verify(accountRepository, never()).findByAccountNumber(DESTINATION_ACCOUNT_NUMBER);
        verifyNoTransferOrTransactionWasSaved();

    }

    /*
    Comprueba que no se puede crear transferencia si la cuenta destino no existe
     */
    @Test
    void shouldThrowWhenAccountDestinationNotFound() {
        User user = buildUser(1L, USER_EMAIL, "Alejandro Hernandez");
        Account sourceAccount = buildAccount(10L, SOURCE_ACCOUNT_NUMBER, AccountType.CHECKING, "1000.00", true, user);
        CreateTransferRequest request = buildValidRequest();
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT_NUMBER)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber(DESTINATION_ACCOUNT_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.createTransfer(USER_EMAIL, request))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Destination account number not found");
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, times(1)).findByAccountNumber(SOURCE_ACCOUNT_NUMBER);
        verify(accountRepository, times(1)).findByAccountNumber(DESTINATION_ACCOUNT_NUMBER);
        verifyNoTransferOrTransactionWasSaved();
    }

    /*
    comprueba que un usuario no puede transferir dinero desde una cuenta que no es suya
     */
    @Test
    void shouldThrowWhenSourceAccountDoesNotBelongToUser() {
        User authenticatedUser = buildUser(1L, USER_EMAIL, "Alejandro Hernandez");
        User realSourceOwner = buildUser(2L, "otheruser@test.com", "Other User");
        User destinationOwner = buildUser(3L, "destination@test.com", "Dstination user");
        Account sourceAccount = buildAccount(10L, SOURCE_ACCOUNT_NUMBER, AccountType.CHECKING, "1000.00", true, realSourceOwner);
        Account destinationAccount = buildAccount(20L, DESTINATION_ACCOUNT_NUMBER, AccountType.CHECKING, "500.00", true, destinationOwner);
        CreateTransferRequest request = buildValidRequest();
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(authenticatedUser));
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT_NUMBER)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber(DESTINATION_ACCOUNT_NUMBER)).thenReturn(Optional.of(destinationAccount));

        assertThatThrownBy(() -> transferService.createTransfer(USER_EMAIL, request))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessage("You are not authorized to perform this operation");
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, times(1)).findByAccountNumber(SOURCE_ACCOUNT_NUMBER);
        verify(accountRepository, times(1)).findByAccountNumber(DESTINATION_ACCOUNT_NUMBER);
        verifyNoTransferOrTransactionWasSaved();
    }

    /*
    Comprueba que no se puede hacer una transferencia desde una cuenta a la misma
     */
    @Test
    void shouldThrowWhenSourceAndDestinationAccountAreTheSame() {
        User user = buildUser(1L, USER_EMAIL, "Alejandro Hernandez");
        Account account = buildAccount(10L, SOURCE_ACCOUNT_NUMBER, AccountType.CHECKING, "1000.00", true, user);
        CreateTransferRequest request = new CreateTransferRequest(
                SOURCE_ACCOUNT_NUMBER,
                SOURCE_ACCOUNT_NUMBER,
                AMOUNT,
                "Prueba transferencia"
        );
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transferService.createTransfer(USER_EMAIL, request))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Source and destination accounts cannot be the same");
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, times(2)).findByAccountNumber(SOURCE_ACCOUNT_NUMBER);
        verifyNoTransferOrTransactionWasSaved();
    }

    /*
    comprueba que no se puede hacer transferencia si la cuenta origen está inactiva
     */
    @Test
    void shouldThrowWhenSourceAccountIsInactive() {
        User user = buildUser(1L, USER_EMAIL, "Alejandro Hernandez");
        User destinationUser = buildUser(2L, "destination@test.com", "Destination user");

        Account sourceAccount = buildAccount(10L, SOURCE_ACCOUNT_NUMBER, AccountType.CHECKING, "1000.00", false, user);
        Account destinationAccount = buildAccount(20L, DESTINATION_ACCOUNT_NUMBER, AccountType.CHECKING, "500.00", true, destinationUser);
        CreateTransferRequest request = buildValidRequest();
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT_NUMBER)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber(DESTINATION_ACCOUNT_NUMBER)).thenReturn(Optional.of(destinationAccount));

        assertThatThrownBy(() -> transferService.createTransfer(USER_EMAIL, request))
                .isInstanceOf(InactiveAccountException.class)
                .hasMessage("Account is not active for the source");

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, times(1)).findByAccountNumber(SOURCE_ACCOUNT_NUMBER);
        verify(accountRepository, times(1)).findByAccountNumber(DESTINATION_ACCOUNT_NUMBER);
        verifyNoTransferOrTransactionWasSaved();
    }

    /*
    comprueba que no se puede hacer transferencia si la cuenta destino está inactiva
     */
    @Test
    void shouldThrowWhenDestinationAccountIsInactive() {
        User user = buildUser(1L, USER_EMAIL, "Alejandro Hernandez");
        User destinationUser = buildUser(2L, "source@test.com", "Source user");
        Account sourceAccount = buildAccount(10L, SOURCE_ACCOUNT_NUMBER, AccountType.CHECKING, "1000.00", true, user);
        Account destinationAccount = buildAccount(20L, DESTINATION_ACCOUNT_NUMBER, AccountType.CHECKING, "500.00", false, destinationUser);
        CreateTransferRequest request = buildValidRequest();
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT_NUMBER)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber(DESTINATION_ACCOUNT_NUMBER)).thenReturn(Optional.of(destinationAccount));

        assertThatThrownBy(() -> transferService.createTransfer(USER_EMAIL, request))
                .isInstanceOf(InactiveAccountException.class)
                .hasMessage("Account is not active for the destination");

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, times(1)).findByAccountNumber(SOURCE_ACCOUNT_NUMBER);
        verify(accountRepository, times(1)).findByAccountNumber(DESTINATION_ACCOUNT_NUMBER);
        verifyNoTransferOrTransactionWasSaved();
    }

    /*
    comprueba que no se puede hacer transferencia si el importe es igual a 0
     */
    @Test
    void shouldThrowWhenAmountIsLessThanZero() {
        User user = buildUser(1L, USER_EMAIL, "Alejandro Hernandez");
        User destinationUser = buildUser(2L, "source@test.com", "Source user");
        Account sourceAccount = buildAccount(10L, SOURCE_ACCOUNT_NUMBER, AccountType.CHECKING, "1000.00", true, user);
        Account destinationAccount = buildAccount(20L, DESTINATION_ACCOUNT_NUMBER, AccountType.CHECKING, "500.00", true, destinationUser);
        CreateTransferRequest request = new CreateTransferRequest(
                SOURCE_ACCOUNT_NUMBER,
                DESTINATION_ACCOUNT_NUMBER,
                BigDecimal.ZERO,
                "Prueba transferencia"
        );
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT_NUMBER)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber(DESTINATION_ACCOUNT_NUMBER)).thenReturn(Optional.of(destinationAccount));

        assertThatThrownBy(() -> transferService.createTransfer(USER_EMAIL, request))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Amount must be greater than 0");
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, times(1)).findByAccountNumber(SOURCE_ACCOUNT_NUMBER);
        verify(accountRepository, times(1)).findByAccountNumber(DESTINATION_ACCOUNT_NUMBER);
        verifyNoTransferOrTransactionWasSaved();

    }
    /*
    no se puede hacer una transferencia si la cuenta origen no tiene dinero suficiente
     */

    @Test
    void shouldThrowWhenInsufficientFunds() {
        User user = buildUser(1L, USER_EMAIL, "Alejandro Hernandez");
        User destinationUser = buildUser(2L, "source@test.com", "Source user");
        Account sourceAccount = buildAccount(10L, SOURCE_ACCOUNT_NUMBER, AccountType.CHECKING, "100.00", true, user);
        Account destinationAccount = buildAccount(20L, DESTINATION_ACCOUNT_NUMBER, AccountType.CHECKING, "500.00", true, destinationUser);
        CreateTransferRequest request = buildValidRequest();
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT_NUMBER)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber(DESTINATION_ACCOUNT_NUMBER)).thenReturn(Optional.of(destinationAccount));

        assertThatThrownBy(() -> transferService.createTransfer(USER_EMAIL, request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessage("Not enough money");
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(accountRepository, times(1)).findByAccountNumber(SOURCE_ACCOUNT_NUMBER);
        verify(accountRepository, times(1)).findByAccountNumber(DESTINATION_ACCOUNT_NUMBER);
        verifyNoTransferOrTransactionWasSaved();
    }

    /*
    comprueba que el usuario puede consultar una transferencia cuando es el dueño de la cuenta origen
     */
    @Test
    void shouldGetTransferByIdWhenUserIsSourceOwner() {
        User sourceOwner = buildUser(1L, USER_EMAIL, "Alejandro Hernandez");
        User destinationOwner = buildUser(2L, "destination@test.com", "Destination user");
        Account sourceAccount = buildAccount(10L, SOURCE_ACCOUNT_NUMBER, AccountType.CHECKING, "750.00", true, sourceOwner);
        Account destinationAccount = buildAccount(10L, DESTINATION_ACCOUNT_NUMBER, AccountType.SAVINGS, "750.00", true, destinationOwner);
        Transfer transfer = buildTransfer(100L, sourceAccount, destinationAccount);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(sourceOwner));
        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));

        TransferResponse response = transferService.getTransferById(USER_EMAIL, 100L);
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.sourceAccountNumber()).isEqualTo(SOURCE_ACCOUNT_NUMBER);
        assertThat(response.destinationAccountNumber()).isEqualTo(DESTINATION_ACCOUNT_NUMBER);
        assertThat(response.sourceFullName()).isEqualTo("Alejandro Hernandez");
        assertThat(response.amount()).isEqualByComparingTo("250.00");
        assertThat(response.description()).isEqualTo("Prueba transferencia");
        assertThat(response.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(transferRepository, times(1)).findById(100L);

    }
    /*
    comprueba que el usuario puede consultar una transferencia cuando es el dueño de la cuenta destino
     */
    @Test
    void shouldGetTransferByIdWhenUserIsDestinationOwner() {
        User sourceOwner = buildUser(1L, "source@test.com", "Source User");
        User destinationOwner = buildUser(2L, USER_EMAIL, "Alejandro Hernandez");

        Account sourceAccount = buildAccount(
                10L,
                SOURCE_ACCOUNT_NUMBER,
                AccountType.CHECKING,
                "750.00",
                true,
                sourceOwner
        );

        Account destinationAccount = buildAccount(
                20L,
                DESTINATION_ACCOUNT_NUMBER,
                AccountType.SAVINGS,
                "750.00",
                true,
                destinationOwner
        );

        Transfer transfer = buildTransfer(100L, sourceAccount, destinationAccount);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(destinationOwner));
        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));

        TransferResponse response = transferService.getTransferById(USER_EMAIL, 100L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.sourceAccountNumber()).isEqualTo(SOURCE_ACCOUNT_NUMBER);
        assertThat(response.destinationAccountNumber()).isEqualTo(DESTINATION_ACCOUNT_NUMBER);
        assertThat(response.sourceFullName()).isEqualTo("Source User");
        assertThat(response.amount()).isEqualByComparingTo("250.00");
        assertThat(response.description()).isEqualTo("Prueba transferencia");
        assertThat(response.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(transferRepository, times(1)).findById(100L);
    }
    /*
    comprueba que no se puede consultar una transferencia si el usuario autenticado no existe
     */
    @Test
    void shouldThrowWhenUserNotFoundGettingTransferById() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.getTransferById(USER_EMAIL, 100L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(transferRepository, never()).findById(anyLong());
    }

    /*
    comprueba que no se puede consultar una transferencia que no existe
     */
    @Test
    void shouldThrowWhenTransferNotFound() {
        User user = buildUser(1L, USER_EMAIL, "Alejandro Hernandez");

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(transferRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.getTransferById(USER_EMAIL, 100L))
                .isInstanceOf(TransferNotFoundException.class)
                .hasMessage("Transfer not found");

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(transferRepository, times(1)).findById(100L);
    }

    /*
    comprueba que un usuario no puede consultar una transferencia en la que no participa
     */
    @Test
    void shouldThrowWhenUserDoesNotBelongToTransfer() {
        User authenticatedUser = buildUser(99L, USER_EMAIL, "Alejandro Hernandez");
        User sourceOwner = buildUser(1L, "source@test.com", "Source User");
        User destinationOwner = buildUser(2L, "destination@test.com", "Destination User");

        Account sourceAccount = buildAccount(
                10L,
                SOURCE_ACCOUNT_NUMBER,
                AccountType.CHECKING,
                "750.00",
                true,
                sourceOwner
        );

        Account destinationAccount = buildAccount(
                20L,
                DESTINATION_ACCOUNT_NUMBER,
                AccountType.SAVINGS,
                "750.00",
                true,
                destinationOwner
        );

        Transfer transfer = buildTransfer(100L, sourceAccount, destinationAccount);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(authenticatedUser));
        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));

        assertThatThrownBy(() -> transferService.getTransferById(USER_EMAIL, 100L))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessage("You are not authorized to perform this operation");

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(transferRepository, times(1)).findById(100L);
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

    private Account buildAccount(Long id, String accountNumber, AccountType accountType, String balance, boolean active, User user) {
        return Account.builder()
                .id(id)
                .accountNumber(accountNumber)
                .accountType(accountType)
                .balance(new BigDecimal(balance))
                .active(active)
                .user(user)
                .build();
    }

    private CreateTransferRequest buildValidRequest() {
        return new CreateTransferRequest(
                SOURCE_ACCOUNT_NUMBER,
                DESTINATION_ACCOUNT_NUMBER,
                AMOUNT,
                "Prueba transferencia correctamente"
        );
    }

    private Transfer buildTransfer(Long id, Account sourceAccount, Account destinationAccount) {
        return Transfer.builder()
                .id(id)
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .amount(AMOUNT)
                .description("Prueba transferencia")
                .status(TransferStatus.COMPLETED)
                .createdAt(CREATED_AT)
                .build();
    }

    public void verifyNoTransferOrTransactionWasSaved() {
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(transferRepository, never()).save(any(Transfer.class));
    }

}
