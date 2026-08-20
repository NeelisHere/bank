package com.bank.common_lib.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransactionRequest(

        @NotBlank(message = "Sender account number is required")
        String senderAccountNumber,

        @NotBlank(message = "Receiver account number is required")
        String receiverAccountNumber,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be a positive value")
        BigDecimal amount
) {
}
