package com.alejandro.banking_api.service;

import com.alejandro.banking_api.dto.CreateTransferRequest;
import com.alejandro.banking_api.dto.TransferResponse;
import com.alejandro.banking_api.entity.*;
import com.alejandro.banking_api.exception.*;
import com.alejandro.banking_api.repository.AccountRepository;
import com.alejandro.banking_api.repository.TransactionRepository;
import com.alejandro.banking_api.repository.TransferRepository;
import com.alejandro.banking_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferService {
    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public TransferResponse createTransfer(String email, CreateTransferRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Account sourceAccount = accountRepository.findByAccountNumber(request.sourceAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Source account number not found"));

        Account destinationAccount = accountRepository.findByAccountNumber(request.destinationAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Destination account number not found"));

        if (!sourceAccount.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("You are not authorized to perform this operation");
        }
        if (sourceAccount.getAccountNumber().equals(destinationAccount.getAccountNumber())) {
            throw new InvalidTransferException("Source and destination accounts cannot be the same");
        }
        if (!sourceAccount.isActive()) {
            throw new InactiveAccountException("Account is not active for the source");
        }
        if (!destinationAccount.isActive()) {
            throw new InactiveAccountException("Account is not active for the destination");
        }
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Amount must be greater than 0");
        }
        if (sourceAccount.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("Not enough money");
        }
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.amount()));
        destinationAccount.setBalance(destinationAccount.getBalance().add(request.amount()));

        Transfer transfer = Transfer.builder()
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .amount(request.amount())
                .description(request.description())
                .status(TransferStatus.COMPLETED)
                .build();

        Transfer savedTransfer = transferRepository.save(transfer);

        Transaction debitTransaction = Transaction.builder()
                .type(TransactionType.DEBIT)
                .amount(request.amount())
                .description(request.description())
                .account(sourceAccount)
                .transfer(savedTransfer)
                .build();

        Transaction creditTransaction = Transaction.builder()
                .type(TransactionType.CREDIT)
                .amount(request.amount())
                .description(request.description())
                .account(destinationAccount)
                .transfer(savedTransfer)
                .build();

        transactionRepository.save(debitTransaction);
        transactionRepository.save(creditTransaction);

        return mapToResponse(savedTransfer);
    }

    public TransferResponse getTransferById(String email, Long id) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new TransferNotFoundException("Transfer not found"));

        boolean isSourceOwner = transfer.getSourceAccount().getUser().getId().equals(user.getId());
        boolean isDestinationOwner = transfer.getDestinationAccount().getUser().getId().equals(user.getId());

        if (!isSourceOwner && !isDestinationOwner) {
            throw new UnauthorizedAccessException("You are not authorized to perform this operation");
        }
        return mapToResponse(transfer);

    }

    private TransferResponse mapToResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceAccount().getAccountNumber(),
                transfer.getDestinationAccount().getAccountNumber(),
                transfer.getSourceAccount().getUser().getFullName(),
                transfer.getAmount(),
                transfer.getDescription(),
                transfer.getStatus(),
                transfer.getCreatedAt()
        );
    }
}
