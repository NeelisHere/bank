package com.bank.common_lib.events;

public record FraudCheckCleanEvent(
        String transactionId,
        boolean isFraud,
        String reason
) {
    public FraudCheckCleanEvent(String transactionId) {
        this(transactionId, false, null);
    }
}
