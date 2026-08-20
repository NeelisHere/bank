package com.bank.notification_service.config;

import com.google.pubsub.v1.ProjectSubscriptionName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcpConfig {
    @Value("${gcp.project-id}")
    private String gcpProjectId;

    @Value("${gcp.pubsub.transaction.completed.subscription}")
    private String transactionCompletedSubscriptionName;

    @Value("${gcp.pubsub.transaction.refunded.subscription}")
    private String transactionRefundedSubscriptionName;

    @Value("${gcp.pubsub.transaction.otp.generated.subscription}")
    private String transactionOtpGeneratedSubscriptionName;

    @Value("${gcp.pubsub.fraud.detected.subscription}")
    private String fraudDetectedSubscriptionName;


    @Bean(name = "transactionCompletedSubscriptionName")
    public ProjectSubscriptionName transactionCompletedSubscriptionName() {
        return ProjectSubscriptionName.of(gcpProjectId, transactionCompletedSubscriptionName);
    }

    @Bean(name = "transactionRefundedSubscriptionName")
    public ProjectSubscriptionName transactionRefundedSubscriptionName() {
        return ProjectSubscriptionName.of(gcpProjectId, transactionRefundedSubscriptionName);
    }

    @Bean(name = "transactionOtpGeneratedSubscriptionName")
    public ProjectSubscriptionName transactionOtpGeneratedSubscriptionName() {
        return ProjectSubscriptionName.of(gcpProjectId, transactionOtpGeneratedSubscriptionName);
    }

    @Bean(name = "fraudDetectedSubscriptionName")
    public ProjectSubscriptionName fraudDetectedSubscriptionName() {
        return ProjectSubscriptionName.of(gcpProjectId, fraudDetectedSubscriptionName);
    }
}
