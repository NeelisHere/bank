package com.bank.common_lib.events;

public record FraudDetectedEvent(
        String transactionId,
        String accountNumber,
        String reason
) { }
