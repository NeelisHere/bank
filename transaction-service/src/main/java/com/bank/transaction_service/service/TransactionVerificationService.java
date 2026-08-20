package com.bank.transaction_service.service;

import com.bank.common_lib.dto.response.TransactionResponse;
import com.bank.common_lib.enums.TransactionStatus;
import com.bank.common_lib.events.VerificationEvent;
import com.bank.transaction_service.entity.Transaction;
import com.bank.transaction_service.mapper.TransactionMapper;
import com.bank.transaction_service.mapper.VerificationEventMapper;
import com.bank.transaction_service.publisher.TransactionOtpGeneratedEventPublisher;
import com.bank.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class TransactionVerificationService {
    private final TransactionRepository transactionRepository;
    private final TransactionQueryService transactionQueryService;
    private final TransactionService transactionService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final StringRedisTemplate redisTemplate;
    private final TransactionOtpGeneratedEventPublisher transactionOtpGeneratedEventPublisher;

    public void handleVerificationRequired(VerificationEvent verificationEvent) {
        UUID transactionId = UUID.fromString(verificationEvent.transactionId());

        Transaction transaction = transactionQueryService.findByIdOrThrow(transactionId);

        // Move to PENDING_VERIFICATION — awaiting user OTP input
        transaction.setTransactionStatus(TransactionStatus.PENDING_VERIFICATION);
        log.info("transaction state: {}", transaction);
        transactionRepository.saveAndFlush(transaction);

        String otp = generateOTP();
        String otpKey = "verification:otp:" + transactionId;
        redisTemplate.opsForValue().set(otpKey, otp, 120, TimeUnit.SECONDS);
        log.info("OTP generated for transaction {}, valid for 2 mins", transactionId);

        transactionOtpGeneratedEventPublisher.publish(VerificationEventMapper.toOtpEvent(verificationEvent, otp));
        log.info("OTP event published for transaction {}", transactionId);
    }

    public TransactionResponse verifyOtp(UUID transactionId, String otp) {
        Transaction transaction = transactionQueryService.findByIdOrThrow(transactionId);

        String otpKey = String.format("verification:otp:%s", transactionId.toString());
        String storedOtp = redisTemplate.opsForValue().get(otpKey);
        if (storedOtp == null) {
            log.warn("OTP expired for transactionId: {}!", transactionId);
            transactionService.compensateTransaction(transaction, "OTP expired - transaction cancelled and amount refunded!");
            return TransactionMapper.toResponse(transaction);
        }
        if (!storedOtp.equals(otp)) {
            log.warn("Wrong OTP - blocking account and refunding: {}", transactionId);
            redisTemplate.delete(otpKey);
            transactionService.blockAccountAndCompensate(transaction, "Wrong OTP: transaction cancelled, account blocked for security!");
            return TransactionMapper.toResponse(transaction);
        }
        redisTemplate.delete(otpKey);
        transactionService.completeTransaction(transaction);
        return TransactionMapper.toResponse(transaction);
    }


    public String generateOTP() {
        return String.format("%06d", secureRandom.nextInt(900_000) + 100_000);
    }
}
