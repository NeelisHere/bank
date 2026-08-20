package com.bank.transaction_service.service;

import com.bank.common_lib.dto.request.TransactionRequest;
import com.bank.common_lib.dto.response.TransactionResponse;
import com.bank.common_lib.enums.TransactionStatus;
import com.bank.common_lib.enums.TransactionType;
import com.bank.common_lib.events.FraudDetectedEvent;
import com.bank.common_lib.events.RefundEvent;
import com.bank.common_lib.events.TransactionEvent;
import com.bank.transaction_service.client.AccountServiceClient;
import com.bank.transaction_service.entity.Transaction;
import com.bank.transaction_service.mapper.TransactionEventMapper;
import com.bank.transaction_service.mapper.TransactionMapper;
import com.bank.transaction_service.publisher.FraudDetectedEventPublisher;
import com.bank.transaction_service.publisher.TransactionCompletedEventPublisher;
import com.bank.transaction_service.publisher.TransactionInitiatedEventPublisher;
import com.bank.transaction_service.publisher.TransactionRefundedEventPublisher;
import com.bank.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionCompletedEventPublisher transactionCompletedEventPublisher;
    private final TransactionRefundedEventPublisher transactionRefundedEventPublisher;
    private final TransactionInitiatedEventPublisher transactionInitiatedEventPublisher;
    private final FraudDetectedEventPublisher fraudDetectedEventPublisher;

    private final TransactionRepository transactionRepository;

    private final AccountServiceClient accountServiceClient;

    private final TransactionQueryService transactionQueryService;

    /*
    * deduct amount from sender via RestClient
    * save transaction as processing
    * publish event in pub/sub for fraud check
    * return transaction
    * */
    public TransactionResponse transfer(TransactionRequest request) {
        log.info("Transferring amount={}INR from {} -> {}", request.amount(), request.senderAccountNumber(), request.receiverAccountNumber());

        // Save as PENDING before attempting deduction
        Transaction transaction = TransactionMapper.toEntity(request);
        transaction.setTransactionStatus(TransactionStatus.PENDING);
        transaction.setTransactionType(TransactionType.TRANSFER);
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("transaction: {} saved as {}", savedTransaction.getId(), savedTransaction.getTransactionStatus());

        try {
            accountServiceClient.deductAmount(request.senderAccountNumber(), request.amount());
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            log.error("Deduction failed for transaction {}: {}", savedTransaction.getId(), e.getResponseBodyAsString());
            savedTransaction.setTransactionStatus(TransactionStatus.FAILED);
            savedTransaction.setFailureReason(e.getStatusCode() + " - " + e.getResponseBodyAsString());
            transactionRepository.saveAndFlush(savedTransaction);
            return TransactionMapper.toResponse(savedTransaction);
        } catch (Exception e) {
            log.error("Unexpected error during deduction for transaction {}: {}", savedTransaction.getId(), e.getMessage());
            savedTransaction.setTransactionStatus(TransactionStatus.FAILED);
            savedTransaction.setFailureReason(e.getMessage());
            transactionRepository.saveAndFlush(savedTransaction);
            return TransactionMapper.toResponse(savedTransaction);
        }

        // Deduction succeeded — move to PROCESSING and initiate fraud check
        savedTransaction.setTransactionStatus(TransactionStatus.PROCESSING);
        transactionRepository.saveAndFlush(savedTransaction);

        TransactionEvent transactionEvent = TransactionEventMapper.toEvent(savedTransaction);
        transactionInitiatedEventPublisher.publish(transactionEvent);

        return TransactionMapper.toResponse(savedTransaction);
    }

    public void completeTransaction(UUID transactionId) {
        Transaction transaction = transactionQueryService.findByIdOrThrow(transactionId);
        completeTransaction(transaction);
    }

    public void completeTransaction(Transaction transaction) {
        if (!TransactionStatus.PROCESSING.equals(transaction.getTransactionStatus())
                && !TransactionStatus.PENDING_VERIFICATION.equals(transaction.getTransactionStatus())) {
            log.info("transaction is not in a completable state ({}), skipping. transaction: {}", transaction.getTransactionStatus(), transaction.getId());
            return;
        }
        transaction.setTransactionStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.saveAndFlush(transaction);

        TransactionEvent transactionCompletedEvent = TransactionEventMapper.toEvent(transaction);
        transactionCompletedEventPublisher.publish(transactionCompletedEvent);
        log.info("SAGA COMPLETE - Transaction: {} completed", transaction.getId());
    }


    public void compensateTransaction(Transaction transaction, String reason) {
        try {
            accountServiceClient.creditAmount(transaction.getSenderAccountNumber(), transaction.getAmount());
            transaction.setTransactionStatus(TransactionStatus.FLAGGED);
            transaction.setFailureReason(reason);
            transactionRepository.saveAndFlush(transaction);
            RefundEvent refundEvent = new RefundEvent(
                    transaction.getId().toString(),
                    transaction.getSenderAccountNumber(),
                    transaction.getAmount(),
                    reason
            );
            transactionRefundedEventPublisher.publish(refundEvent);
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            log.info("ERROR: {}, status_code: {}", e.getResponseBodyAsString(), e.getStatusCode());
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }

    public void blockAccountAndCompensate(Transaction transaction, String reason) {
        FraudDetectedEvent fraudEvent = new FraudDetectedEvent(
                transaction.getId().toString(),
                transaction.getSenderAccountNumber(),
                reason
        );
        fraudDetectedEventPublisher.publish(fraudEvent);
        log.warn("fraud.detected published - account: {} will be blocked, Kindly contact the bank",
                transaction.getSenderAccountNumber());

        compensateTransaction(transaction, reason);
    }

}
