package com.bank.transaction_service.mapper;

import com.bank.common_lib.events.TransactionEvent;
import com.bank.transaction_service.entity.Transaction;

public class TransactionEventMapper {

    private TransactionEventMapper() {}

    public static TransactionEvent toEvent(Transaction transaction) {
        return new TransactionEvent(
                transaction.getId().toString(),
                transaction.getSenderAccountNumber(),
                transaction.getReceiverAccountNumber(),
                transaction.getAmount()
        );
    }
}
