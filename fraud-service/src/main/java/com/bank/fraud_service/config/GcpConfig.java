package com.bank.fraud_service.config;

import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.TopicName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcpConfig {

    @Value("${gcp.project-id}")
    private String gcpProjectId;

    @Value("${gcp.pubsub.verification.required.topic}")
    private String verificationRequiredTopic;

    @Value("${gcp.pubsub.fraud.check.clean.topic}")
    private String fraudCheckCleanTopic;

    @Value("${gcp.pubsub.transaction.initiated.subscription}")
    private String transactionInitiatedSubscription;

    @Bean(name = "verificationRequiredTopicName")
    public TopicName verificationRequiredTopicName() {
        return TopicName.of(gcpProjectId, verificationRequiredTopic);
    }

    @Bean(name = "fraudCheckCleanTopicName")
    public TopicName fraudCheckCleanTopicName() {
        return TopicName.of(gcpProjectId, fraudCheckCleanTopic);
    }

    @Bean(name = "transactionInitiatedSubscriptionName")
    public ProjectSubscriptionName transactionInitiatedSubscriptionName() {
        return ProjectSubscriptionName.of(gcpProjectId, transactionInitiatedSubscription);
    }
}
