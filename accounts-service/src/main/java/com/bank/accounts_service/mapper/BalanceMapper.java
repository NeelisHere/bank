package com.bank.accounts_service.mapper;

import com.bank.accounts_service.entity.Account;
import com.bank.common_lib.dto.response.BalanceResponse;

public class BalanceMapper {

    private BalanceMapper() {}

    public static BalanceResponse toResponse(Account account) {
        return BalanceResponse.builder()
                .accountNumber(account.getAccountNumber())
                .accountHolderName(account.getAccountHolderName())
                .balance(account.getBalance())
                .build();
    }
}
