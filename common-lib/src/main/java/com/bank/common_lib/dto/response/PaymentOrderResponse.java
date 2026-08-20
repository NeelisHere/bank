package com.bank.common_lib.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentOrderResponse {
    String paymentId;
    String razorpayOrderId;
    BigDecimal amount;
    String currency;
    String razorpayKeyId;
    String status;
}
