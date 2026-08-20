package com.bank.common_lib.events;

import java.math.BigDecimal;

public record OtpEvent(
        String transactionId,
        String accountNumber,
        String reason,
        String otp,
        BigDecimal amount
) {}
