package com.bank.common_lib.dto.response;

import com.bank.common_lib.enums.TransactionStatus;
import com.bank.common_lib.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TransactionResponse {
    private UUID id;
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private BigDecimal amount;
    private TransactionType transactionType;
    private TransactionStatus transactionStatus;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
