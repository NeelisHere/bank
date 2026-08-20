package com.bank.fraud_service.client;

import com.bank.common_lib.dto.response.BalanceResponse;
import com.bank.common_lib.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountServiceClient {

    private final RestClient restClient;

    public BalanceResponse getAccountBalance(String accountNumber) {
        try {
            BalanceResponse response = restClient.get()
                    .uri("/api/v1/accounts/{accountNumber}/balance", accountNumber)
                    .retrieve()
                    .body(BalanceResponse.class);
            log.info("Fetched balance for account {}", accountNumber);
            return response;
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Client/server error fetching balance for account {}: {}", accountNumber, ex.getMessage());
            return null;
        }
    }
}
