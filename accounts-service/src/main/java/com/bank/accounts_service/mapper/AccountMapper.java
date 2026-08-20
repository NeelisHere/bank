package com.bank.accounts_service.mapper;

import com.bank.accounts_service.entity.Account;
import com.bank.accounts_service.utils.AccountUtils;
import com.bank.common_lib.dto.request.CreateAccountRequest;
import com.bank.common_lib.dto.response.AccountResponse;
import com.bank.common_lib.enums.AccountStatus;
import com.bank.common_lib.enums.AccountType;

public class AccountMapper {

    private AccountMapper() {}

    public static Account toEntity(CreateAccountRequest request) {
        return Account.builder()
                .accountHolderName(request.accountHolderName())
                .email(request.email())
                .phone(request.phone())
                .accountType(request.accountType())
                .accountStatus(AccountStatus.ACTIVE)
                .balance(request.initialDeposit())
                .build();
    }

    public static AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountHolderName(account.getAccountHolderName())
                .email(account.getEmail())
                .phone(account.getPhone())
                .accountType(account.getAccountType())
                .accountStatus(account.getAccountStatus())
                .balance(account.getBalance())
                .dailyTransactionLimit(account.getDailyTransactionLimit())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
