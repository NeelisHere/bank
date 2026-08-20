package com.bank.fraud_service.service;

import com.bank.common_lib.dto.response.BalanceResponse;
import com.bank.common_lib.dto.response.FraudCheckResponse;
import com.bank.common_lib.events.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionPipeline {
    private final StringRedisTemplate redisTemplate;

    @Value("${fraud.max-transactions-per-minute}")
    private int maxTransactionsPerMinute;

    @Value("${fraud.suspicious-amount-multiplier}")
    private double suspiciousAmountMultiplier;

    @Value("${fraud.max-balance-percentage}")
    private double maxBalancePercentage;

    public FraudCheckResponse performFraudChecks(TransactionEvent transactionEvent, BalanceResponse senderBalanceResponse) {
        BigDecimal amount = transactionEvent.amount();
        String senderAccountNumber = transactionEvent.senderAccountNumber();
        BigDecimal senderBalance = senderBalanceResponse.getBalance();

        if (!isTransactionCountWithinMaxAllowedTransactions(senderAccountNumber)) {
            return new FraudCheckResponse(true, "Too many transactions in a 60s window!");
        }

        if (!isTransactionAmountLessThanAverageThreshold(senderAccountNumber, amount)) {
            return new FraudCheckResponse(true, String.format("transaction amount exceeds %sx the " +
                    "avg of the prev transaction amounts so far!", suspiciousAmountMultiplier)
            );
        }

        if (!isTransactionAmountWithinBalancePercentageLimit(senderBalance, amount)) {
            return new FraudCheckResponse(true, String.format("transaction amount is %sx the balance", maxBalancePercentage));
        }

        return new FraudCheckResponse(false, null);
    }

    private boolean isTransactionCountWithinMaxAllowedTransactions(String accountNumber) {
        String key = "fraud:velocity:" + accountNumber;
        Long transactionCount = redisTemplate.opsForValue().increment(key);
        if (transactionCount == null) {
            log.info("Some issue with Redis connection");
            return false;
        }
        if(transactionCount == 1){
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }
        log.info("velocity check - account: {}, count: {}/{}", accountNumber, transactionCount, maxTransactionsPerMinute);
        return transactionCount <= maxTransactionsPerMinute;
    }

    private boolean isTransactionAmountLessThanAverageThreshold(String accountNumber, BigDecimal amount) {
        String avgKey = "fraud:avg-key:" + accountNumber;
        String avgVal = redisTemplate.opsForValue().get(avgKey);
        if (avgVal == null) {
            redisTemplate.opsForValue().set(avgKey, amount.toString());
            return true;
        }
        BigDecimal avgAmountSoFar = new BigDecimal(avgVal);
        BigDecimal maxAmountAllowed = avgAmountSoFar.multiply(BigDecimal.valueOf(suspiciousAmountMultiplier));

        BigDecimal newRunningAvg = avgAmountSoFar.add(amount).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        redisTemplate.opsForValue().set(avgKey, newRunningAvg.toString());

        log.info("transaction amount={}, allowed amount={}, suspicious={}", amount, maxAmountAllowed, maxAmountAllowed.compareTo(amount) < 0);
        return maxAmountAllowed.compareTo(amount) >= 0;
    }

    private boolean isTransactionAmountWithinBalancePercentageLimit(BigDecimal senderBalance, BigDecimal amount) {
        BigDecimal maxAmountAllowed = senderBalance.multiply(BigDecimal.valueOf(maxBalancePercentage));
        log.info("transaction amount={}, balance={}, allowedFraction={} maxAmountAllowed={} suspicious={}",
                amount, senderBalance, maxBalancePercentage, maxAmountAllowed, maxAmountAllowed.compareTo(amount) < 0
        );
        return maxAmountAllowed.compareTo(amount) >= 0;
    }
}
