package com.bank.common_lib.dto.response;

import com.bank.common_lib.enums.AccountStatus;
import com.bank.common_lib.enums.AccountType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountResponse {
    UUID id;
    String accountNumber;
    String accountHolderName;
    String email;
    String phone;
    AccountType accountType;
    AccountStatus accountStatus;
    BigDecimal balance;
    BigDecimal dailyTransactionLimit;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
