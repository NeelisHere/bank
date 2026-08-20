package com.bank.transaction_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${accounts.service.url}")
    private String accountsServiceUrl;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(accountsServiceUrl)
                .build();
    }
}
