package com.alejandro.banking_api.controller;

import com.alejandro.banking_api.dto.AccountResponse;
import com.alejandro.banking_api.dto.CreateAccountRequest;
import com.alejandro.banking_api.dto.TransactionResponse;
import com.alejandro.banking_api.service.AccountService;
import com.alejandro.banking_api.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(Authentication authentication, @Valid @RequestBody CreateAccountRequest createAccountRequest) {
        String email =authentication.getName();

        AccountResponse accountResponse = accountService.createAccount(email,createAccountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountResponse);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Authentication authentication) {
        String email = authentication.getName();
        List<AccountResponse> accountResponse = accountService.getMyAccounts(email);
        return ResponseEntity.status(HttpStatus.OK).body(accountResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getMyAccountById(Authentication authentication, @PathVariable Long id) {
        String email = authentication.getName();
        AccountResponse accountResponse = accountService.getMyAccountById(email, id);
        return ResponseEntity.ok(accountResponse);
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByAccountId(Authentication authentication, @PathVariable Long id) {
        String email = authentication.getName();
        List<TransactionResponse> transactions = transactionService.getTransactionsByAccountId(email,id);
        return ResponseEntity.ok(transactions);
    }
}
