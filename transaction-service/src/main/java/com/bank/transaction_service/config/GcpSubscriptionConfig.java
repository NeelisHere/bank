package com.bank.transaction_service.config;

import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.TopicName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcpSubscriptionConfig {
    @Value("${gcp.project-id}")
    private String gcpProjectId;


    @Value("${gcp.pubsub.fraud.check.clean.subscription}")
    private String fraudCheckCleanSubscription;

    @Value("${gcp.pubsub.verification.required.subscription}")
    private String verificationRequiredSubscription;


    @Bean(name = "fraudCheckCleanSubscription")
    public ProjectSubscriptionName fraudCheckCleanSubscription() {
        return ProjectSubscriptionName.of(gcpProjectId, fraudCheckCleanSubscription);
    }

    @Bean(name = "verificationRequiredSubscription")
    public ProjectSubscriptionName verificationRequiredSubscription() {
        return ProjectSubscriptionName.of(gcpProjectId, verificationRequiredSubscription);
    }
}
