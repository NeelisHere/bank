package com.bank.fraud_service.service;

import com.bank.common_lib.dto.response.BalanceResponse;
import com.bank.common_lib.dto.response.FraudCheckResponse;
import com.bank.common_lib.events.FraudCheckCleanEvent;
import com.bank.common_lib.events.TransactionEvent;
import com.bank.common_lib.events.VerificationEvent;
import com.bank.fraud_service.client.AccountServiceClient;
import com.bank.fraud_service.publisher.FraudCheckCleanEventPublisher;
import com.bank.fraud_service.publisher.VerificationRequiredEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudService {
    private final AccountServiceClient accountServiceClient;
    private final FraudDetectionPipeline fraudDetectionPipeline;
    private final VerificationRequiredEventPublisher verificationRequiredEventPublisher;
    private final FraudCheckCleanEventPublisher fraudCheckCleanEventPublisher;

    public void checkTransaction(TransactionEvent transactionEvent) {
        BalanceResponse senderBalanceResponse = accountServiceClient.getAccountBalance(transactionEvent.senderAccountNumber());
        if (senderBalanceResponse == null) {
            return;
        }
        FraudCheckResponse fraudCheckResponse = fraudDetectionPipeline.performFraudChecks(transactionEvent, senderBalanceResponse);
        if (fraudCheckResponse.isFraud()) {
            log.info(
                    "suspicious activity detected, requesting OTP verification! accountNumber: {}, reason: {}",
                    transactionEvent.senderAccountNumber(),
                    fraudCheckResponse.getReason()
            );
            VerificationEvent verificationEvent = new VerificationEvent(
                    transactionEvent.transactionId(),
                    transactionEvent.senderAccountNumber(),
                    transactionEvent.amount(),
                    fraudCheckResponse.getReason()
            );
            verificationRequiredEventPublisher.publish(verificationEvent);
        } else {
            log.info("Transaction clean");
            FraudCheckCleanEvent fraudCheckCleanEvent = new FraudCheckCleanEvent(
                    transactionEvent.transactionId(), false, null
            );
            fraudCheckCleanEventPublisher.publish(fraudCheckCleanEvent);
        }
    }
}
