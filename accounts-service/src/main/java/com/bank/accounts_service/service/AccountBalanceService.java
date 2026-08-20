package com.bank.accounts_service.service;

import com.bank.accounts_service.entity.Account;
import com.bank.accounts_service.repository.AccountRepository;
import com.bank.accounts_service.utils.AccountUtils;
import com.bank.common_lib.dto.response.GenericResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountBalanceService {
    private final AccountRepository accountRepository;
    private final AccountUtils accountUtils;

    @Transactional
    public GenericResponse deductBalance(String accountNumber, BigDecimal amount) {
        // Read for validation only — active check and balance check
        Account account = accountUtils.findByAccountNumberOrThrow(accountNumber);
        accountUtils.checkIfAccountIsActive(account);
        accountUtils.checkIfSufficientBalance(account, amount);

        // Atomic UPDATE: balance = balance - amount at the DB level
        accountRepository.deductBalance(accountNumber, amount);

        return new GenericResponse(
                String.format("Amount: %s deducted from account: %s", amount, accountNumber),
                HttpStatus.OK.name(),
                LocalDateTime.now()
        );
    }

    @Transactional
    public GenericResponse creditBalance(String accountNumber, BigDecimal amount) {
        // Read for validation only — active check
        Account account = accountUtils.findByAccountNumberOrThrow(accountNumber);
        accountUtils.checkIfAccountIsActive(account);

        // Atomic UPDATE: balance = balance + amount at the DB level
        accountRepository.creditBalance(accountNumber, amount);

        return new GenericResponse(
                String.format("Amount: %s credited to account: %s", amount, accountNumber),
                HttpStatus.OK.name(),
                LocalDateTime.now()
        );
    }
}
