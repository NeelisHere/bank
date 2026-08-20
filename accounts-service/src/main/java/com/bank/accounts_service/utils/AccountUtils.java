package com.bank.accounts_service.utils;

import com.bank.accounts_service.entity.Account;
import com.bank.accounts_service.repository.AccountRepository;
import com.bank.common_lib.enums.AccountStatus;
import com.bank.common_lib.exception.AccountNotFoundException;
import com.bank.common_lib.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountUtils {
    public static final BigDecimal SAVINGS_ACCOUNT_TRANSACTION_LIMIT = new BigDecimal("100000");
    public static final BigDecimal FIXED_DEPOSIT_ACCOUNT_TRANSACTION_LIMIT = new BigDecimal("500000");
    private static final SecureRandom secureRandom = new SecureRandom();
    private final AccountRepository accountRepository;

    private String generateRandom12DigitNumber() {
        return String.format("%012d", secureRandom.nextLong(1_000_000_000_000_000L));
    }

    public String generateAccountNumber() {
        String accountNumber = generateRandom12DigitNumber();
        while (accountRepository.existsByAccountNumber(accountNumber)) {
            accountNumber = generateRandom12DigitNumber();
        }
        return accountNumber;
    }

    public Account findByAccountNumberOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    AccountNotFoundException ex = new AccountNotFoundException(accountNumber, HttpStatus.NOT_FOUND.name());
                    log.error(ex.getMessage());
                    return ex;
                });
    }

    public void findByEmailOrThrow(String email) {
        accountRepository.findByEmail(email)
                .ifPresent((account) -> {
                    String message = String.format(
                            "Account for the user %s already exists! Account number: %s ",
                            email,
                            account.getAccountNumber()
                    );
                    log.error(message);
                    throw new CommonException(message, HttpStatus.BAD_REQUEST.name());
                });
    }

    public void checkIfSufficientBalance(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            String message = String.format(
                    "Account: %s does not have sufficient balance. Current balance %s",
                    account.getAccountNumber(),
                    account.getBalance()
            );
            log.error(message);
            throw new CommonException(message, HttpStatus.BAD_REQUEST.name());
        }
    }

    public void checkIfAccountIsActive(Account account) {
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            String message = String.format("Account: %s is not active", account.getAccountNumber());
            log.error(message);
            throw new CommonException(message, HttpStatus.BAD_REQUEST.name());
        }
    }
}

