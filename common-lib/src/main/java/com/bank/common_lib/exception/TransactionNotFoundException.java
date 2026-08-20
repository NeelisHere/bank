package com.bank.common_lib.exception;

import lombok.Getter;

@Getter
public class TransactionNotFoundException extends RuntimeException {
    String status;
    public TransactionNotFoundException(String transactionId, String status) {
        super(String.format("Transaction id: %s not found!", transactionId));
        this.status = status;
    }
}
