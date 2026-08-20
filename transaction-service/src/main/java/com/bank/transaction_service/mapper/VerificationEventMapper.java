package com.bank.transaction_service.mapper;

import com.bank.common_lib.events.OtpEvent;
import com.bank.common_lib.events.VerificationEvent;

public class VerificationEventMapper {

    private VerificationEventMapper() {}

    public static OtpEvent toOtpEvent(VerificationEvent verificationEvent, String otp) {
        return new OtpEvent(
                verificationEvent.transactionId(),
                verificationEvent.accountNumber(),
                verificationEvent.reason(),
                otp,
                verificationEvent.amount()
        );
    }
}
