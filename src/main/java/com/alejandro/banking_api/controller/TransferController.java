package com.alejandro.banking_api.controller;

import com.alejandro.banking_api.dto.CreateTransferRequest;
import com.alejandro.banking_api.dto.TransferResponse;
import com.alejandro.banking_api.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {
    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(Authentication authentication,
                                                          @Valid @RequestBody CreateTransferRequest request) {
        String email = authentication.getName();
        TransferResponse transferResponse = transferService.createTransfer(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(transferResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getTransferById(Authentication authentication, @PathVariable Long id) {
        String email = authentication.getName();
        TransferResponse transferResponse = transferService.getTransferById(email, id);
        return ResponseEntity.ok(transferResponse);
    }
}
