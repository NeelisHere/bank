package com.bank.accounts_service.config;

import com.google.pubsub.v1.ProjectSubscriptionName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcpConfig {
    @Value("${gcp.project-id}")
    private String gcpProjectId;

    @Value("${gcp.pubsub.transaction.completed.accounts.subscription}")
    private String transactionCompletedSubscriptionName;

    @Value("${gcp.pubsub.fraud.detected.subscription}")
    private String fraudDetectedSubscriptionName;

    @Bean(name = "transactionCompletedSubscriptionName")
    public ProjectSubscriptionName transactionCompletedSubscriptionName() {
        return ProjectSubscriptionName.of(gcpProjectId, transactionCompletedSubscriptionName);
    }

    @Bean(name = "fraudDetectedSubscriptionName")
    public ProjectSubscriptionName fraudDetectedSubscriptionName() {
        return ProjectSubscriptionName.of(gcpProjectId, fraudDetectedSubscriptionName);
    }
}
