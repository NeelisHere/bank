package com.bank.payment_service.mapper;

import com.bank.common_lib.dto.request.CreatePaymentRequest;
import com.bank.common_lib.dto.response.PaymentOrderResponse;
import com.bank.common_lib.enums.PaymentStatus;
import com.bank.payment_service.model.Payment;

public class PaymentMapper {

    private PaymentMapper() {}

    public static Payment toEntity(CreatePaymentRequest request, String razorpayOrderId, String currency) {
        return Payment.builder()
                .accountNumber(request.accountNumber())
                .amount(request.amount())
                .currency(currency)
                .description(request.description())
                .razorpayOrderId(razorpayOrderId)
                .status(PaymentStatus.CREATED)
                .build();
    }

    public static PaymentOrderResponse toResponse(Payment payment, String razorpayKeyId) {
        return PaymentOrderResponse.builder()
                .paymentId(payment.getId().toString())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .razorpayKeyId(razorpayKeyId)
                .status(payment.getStatus().name())
                .build();
    }
}
