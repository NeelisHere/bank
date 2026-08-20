package com.bank.common_lib.events;

import java.math.BigDecimal;

public record PaymentCompletedEvent(
        String paymentId,
        String accountNumber,
        BigDecimal amount,
        String currency,
        String razorpayOrderId,
        String razorpayPaymentId
) {}
