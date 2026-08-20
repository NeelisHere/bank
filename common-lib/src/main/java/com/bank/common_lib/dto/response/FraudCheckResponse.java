package com.bank.common_lib.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FraudCheckResponse {
    private boolean isFraud;
    private String reason;
}
