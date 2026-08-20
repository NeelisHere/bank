package com.bank.accounts_service.entity;

import com.bank.common_lib.enums.AccountStatus;
import com.bank.common_lib.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Builder
@Table(name = "accounts")
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, unique = true)
    String accountNumber;

    @Column(nullable = false)
    String accountHolderName;

    @Column(nullable = false)
    String email;

    @Column(nullable = false)
    String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    AccountStatus accountStatus;

    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal balance;

    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal dailyTransactionLimit;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;
}
