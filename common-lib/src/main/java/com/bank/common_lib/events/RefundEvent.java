package com.bank.common_lib.events;

import java.math.BigDecimal;

public record RefundEvent(
        String transactionId,
        String senderAccountNumber,
        BigDecimal amount,
        String reason
) {}
