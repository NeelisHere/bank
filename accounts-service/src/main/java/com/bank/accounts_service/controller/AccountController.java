package com.bank.accounts_service.controller;

import com.bank.accounts_service.service.AccountBalanceService;
import com.bank.accounts_service.service.AccountManagementService;
import com.bank.accounts_service.service.AccountStatusService;
import com.bank.common_lib.dto.request.CreateAccountRequest;
import com.bank.common_lib.dto.response.AccountResponse;
import com.bank.common_lib.dto.response.BalanceResponse;
import com.bank.common_lib.dto.response.GenericResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountStatusService accountStatusService;
    private final AccountBalanceService accountBalanceService;
    private final AccountManagementService accountManagementService;

    @PostMapping("/create")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountManagementService.createAccount(request));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountManagementService.getAccount(accountNumber));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountManagementService.getBalance(accountNumber));
    }

    @PutMapping("/{accountNumber}/block")
    public ResponseEntity<GenericResponse> blockAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountStatusService.blockAccount(accountNumber));
    }

    @PutMapping("/{accountNumber}/unblock")
    public ResponseEntity<GenericResponse> unblockAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountStatusService.unblockAccount(accountNumber));
    }

    @PutMapping("/{accountNumber}/close")
    public ResponseEntity<GenericResponse> closeAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountStatusService.closeAccount(accountNumber));
    }

    @PutMapping("/{accountNumber}/deduct")
    public ResponseEntity<GenericResponse> deductBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(accountBalanceService.deductBalance(accountNumber, amount));
    }

    @PutMapping("/{accountNumber}/credit")
    public ResponseEntity<GenericResponse> creditBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(accountBalanceService.creditBalance(accountNumber, amount));
    }
}
