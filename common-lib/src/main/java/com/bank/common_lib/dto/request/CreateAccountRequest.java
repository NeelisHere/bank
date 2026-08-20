package com.bank.common_lib.dto.request;

import com.bank.common_lib.enums.AccountType;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank(message = "Account holder name is required")
        String accountHolderName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone number must be valid")
        String phone,

        @NotNull(message = "Account type is required")
        AccountType accountType,

        @NotNull(message = "Initial deposit is required")
        @Positive(message = "Initial deposit must be a positive amount")
        BigDecimal initialDeposit
) {
}
