package com.bank.transaction_service.controller;

import com.bank.common_lib.dto.request.TransactionRequest;
import com.bank.common_lib.dto.response.GenericResponse;
import com.bank.common_lib.dto.response.TransactionResponse;
import com.bank.transaction_service.service.TransactionQueryService;
import com.bank.transaction_service.service.TransactionService;
import com.bank.transaction_service.service.TransactionVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionVerificationService transactionVerificationService;
    private final TransactionService transactionService;
    private final TransactionQueryService transactionQueryService;

    @PostMapping(path = "/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.transfer(request));
    }

    @GetMapping(path = "/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(transactionQueryService.getTransaction(transactionId));
    }

    @GetMapping(path = "/history/{accountNumber}")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(@PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionQueryService.getTransactionHistory(accountNumber));
    }

    @PostMapping(path = "/{transactionId}/verify")
    public ResponseEntity<TransactionResponse> verifyTransaction(
            @PathVariable(name = "transactionId") UUID transactionId,
            @RequestParam(name = "otp") String otp
    ) {
        return ResponseEntity.ok(transactionVerificationService.verifyOtp(transactionId, otp));
    }
}
