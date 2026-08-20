package com.bank.transaction_service.mapper;

import com.bank.common_lib.dto.request.TransactionRequest;
import com.bank.common_lib.dto.response.TransactionResponse;
import com.bank.common_lib.enums.TransactionStatus;
import com.bank.common_lib.enums.TransactionType;
import com.bank.transaction_service.entity.Transaction;

public class TransactionMapper {

    private TransactionMapper() {}

    public static Transaction toEntity(TransactionRequest request) {
        return Transaction.builder()
                .senderAccountNumber(request.senderAccountNumber())
                .receiverAccountNumber(request.receiverAccountNumber())
                .amount(request.amount())
                .build();
    }

    public static TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .senderAccountNumber(transaction.getSenderAccountNumber())
                .receiverAccountNumber(transaction.getReceiverAccountNumber())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .transactionStatus(transaction.getTransactionStatus())
                .failureReason(transaction.getFailureReason())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .build();
    }
}
