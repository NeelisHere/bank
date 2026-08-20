package com.bank.common_lib.events;

import java.math.BigDecimal;

public record PaymentFailedEvent(
        String paymentId,
        String accountNumber,
        BigDecimal amount,
        String currency,
        String razorpayOrderId,
        String failureReason
) {}
