package com.bank.common_lib.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class AccountNotFoundException extends RuntimeException {
    String status;
    public AccountNotFoundException(String accountNumber, String status) {
        super(String.format("Account number: %s not found!", accountNumber));
        this.status = status;
    }
}
