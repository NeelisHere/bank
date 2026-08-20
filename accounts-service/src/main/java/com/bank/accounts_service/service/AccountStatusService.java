package com.bank.accounts_service.service;

import com.bank.accounts_service.entity.Account;
import com.bank.accounts_service.repository.AccountRepository;
import com.bank.accounts_service.utils.AccountUtils;
import com.bank.common_lib.dto.response.GenericResponse;
import com.bank.common_lib.enums.AccountStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountStatusService {
    private final AccountRepository accountRepository;
    private final AccountUtils accountUtils;

    public GenericResponse updateAccountStatus(String accountNumber, AccountStatus accountStatus) {
        Account account = accountUtils.findByAccountNumberOrThrow(accountNumber);
        if (account.getAccountStatus() == accountStatus) {
            return new GenericResponse(
                    String.format("Account: %s already in %s state!", accountNumber, accountStatus.name()),
                    HttpStatus.OK.name(),
                    LocalDateTime.now()
            );
        }
        account.setAccountStatus(accountStatus);
        accountRepository.save(account);
        return new GenericResponse(
                String.format("Account: %s has been moved to %s state successfully!", accountNumber, accountStatus.name()),
                HttpStatus.OK.name(),
                LocalDateTime.now()
        );
    }

    public GenericResponse blockAccount(String accountNumber) {
        return updateAccountStatus(accountNumber, AccountStatus.BLOCKED);
    }

    public GenericResponse unblockAccount(String accountNumber) {
        return updateAccountStatus(accountNumber, AccountStatus.ACTIVE);
    }

    public GenericResponse closeAccount(String accountNumber) {
        return updateAccountStatus(accountNumber, AccountStatus.CLOSED);
    }
}
