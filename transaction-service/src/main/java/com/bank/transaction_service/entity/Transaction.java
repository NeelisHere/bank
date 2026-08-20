package com.bank.transaction_service.entity;

import com.bank.common_lib.enums.TransactionStatus;
import com.bank.common_lib.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false)
    String senderAccountNumber;

    @Column(nullable = false)
    String receiverAccountNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    TransactionStatus transactionStatus;

    String failureReason;

    @CreationTimestamp
    LocalDateTime createdAt;

    LocalDateTime completedAt;
}
