package com.bank.common_lib.events;


import com.bank.common_lib.enums.TransactionStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionEvent(
        String transactionId,
        String senderAccountNumber,
        String receiverAccountNumber,
        BigDecimal amount
) {}
