package com.bank.transaction_service.service;

import com.bank.common_lib.dto.response.TransactionResponse;
import com.bank.common_lib.exception.TransactionNotFoundException;
import com.bank.transaction_service.entity.Transaction;
import com.bank.transaction_service.mapper.TransactionMapper;
import com.bank.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionQueryService {
    private final TransactionRepository transactionRepository;

    public Transaction findByIdOrThrow(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId.toString(), HttpStatus.NOT_FOUND.name()));
    }

    public TransactionResponse getTransaction(UUID transactionId) {
        return TransactionMapper.toResponse(findByIdOrThrow(transactionId));
    }

    public List<TransactionResponse> getTransactionHistory(String accountNumber) {
        return transactionRepository
                .findBySenderAccountNumberOrReceiverAccountNumber(accountNumber, accountNumber)
                .stream()
                .map(TransactionMapper::toResponse)
                .toList();
    }
}
