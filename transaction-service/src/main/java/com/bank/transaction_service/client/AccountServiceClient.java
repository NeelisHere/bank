package com.bank.transaction_service.client;

import com.bank.common_lib.dto.response.BalanceResponse;
import com.bank.common_lib.dto.response.GenericResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountServiceClient {

    private final RestClient restClient;

    public void deductAmount(String accountNumber, BigDecimal amount) {
        restClient.put()
                .uri("/api/v1/accounts/{accountNumber}/deduct?amount={amount}", accountNumber, amount)
                .retrieve()
                .body(GenericResponse.class);
        log.info("Deducted {} from account {}", amount, accountNumber);
    }

    public BigDecimal getAccountBalance(String accountNumber) {
        BalanceResponse response = restClient.get()
                .uri("/api/v1/accounts/{accountNumber}/balance", accountNumber)
                .retrieve()
                .body(BalanceResponse.class);
        log.info("Fetched balance for account {}", accountNumber);
        assert response != null;
        return response.getBalance();
    }

    public void creditAmount(String accountNumber, BigDecimal amount) {
        restClient.put()
                .uri("/api/v1/accounts/{accountNumber}/credit?amount={amount}", accountNumber, amount)
                .retrieve()
                .body(GenericResponse.class);
        log.info("Credited {} to account {}", amount, accountNumber);
    }
}
