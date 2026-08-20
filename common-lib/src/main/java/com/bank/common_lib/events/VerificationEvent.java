package com.bank.common_lib.events;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

public record VerificationEvent (
    String transactionId,
    String accountNumber,
    BigDecimal amount,
    String reason
) {}
