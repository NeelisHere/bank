package com.bank.payment_service.mapper;

import com.bank.common_lib.events.PaymentCompletedEvent;
import com.bank.common_lib.events.PaymentFailedEvent;
import com.bank.payment_service.model.Payment;

public class PaymentEventMapper {

    private PaymentEventMapper() {}

    public static PaymentCompletedEvent toCompletedEvent(Payment payment) {
        return new PaymentCompletedEvent(
                payment.getId(),
                payment.getAccountNumber(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getRazorpayOrderId(),
                payment.getRazorpayPaymentId()
        );
    }

    public static PaymentFailedEvent toFailedEvent(Payment payment) {
        return new PaymentFailedEvent(
                payment.getId(),
                payment.getAccountNumber(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getRazorpayOrderId(),
                payment.getFailureReason()
        );
    }
}
