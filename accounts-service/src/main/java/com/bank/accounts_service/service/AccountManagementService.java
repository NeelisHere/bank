package com.bank.accounts_service.service;

import com.bank.accounts_service.entity.Account;
import com.bank.accounts_service.mapper.AccountMapper;
import com.bank.accounts_service.mapper.BalanceMapper;
import com.bank.accounts_service.repository.AccountRepository;
import com.bank.accounts_service.utils.AccountUtils;
import com.bank.common_lib.dto.request.CreateAccountRequest;
import com.bank.common_lib.dto.response.AccountResponse;
import com.bank.common_lib.dto.response.BalanceResponse;
import com.bank.common_lib.enums.AccountType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountManagementService {
    private final AccountRepository accountRepository;
    private final AccountUtils accountUtils;

    public AccountResponse createAccount(CreateAccountRequest request) {
        accountUtils.findByEmailOrThrow(request.email());

        Account newAccount = AccountMapper.toEntity(request);
        newAccount.setAccountNumber(accountUtils.generateAccountNumber());
        newAccount.setDailyTransactionLimit(
                request.accountType() == AccountType.SAVINGS ?
                        AccountUtils.SAVINGS_ACCOUNT_TRANSACTION_LIMIT :
                        AccountUtils.FIXED_DEPOSIT_ACCOUNT_TRANSACTION_LIMIT
        );

        Account createdAccount = accountRepository.save(newAccount);
        return AccountMapper.toResponse(createdAccount);
    }

    public AccountResponse getAccount(String accountNumber) {
        Account account = accountUtils.findByAccountNumberOrThrow(accountNumber);
        return AccountMapper.toResponse(account);
    }

    public BalanceResponse getBalance(String accountNumber) {
        Account account = accountUtils.findByAccountNumberOrThrow(accountNumber);
        return BalanceMapper.toResponse(account);
    }

}
