package com.bank.transaction_service.config;

import com.google.pubsub.v1.TopicName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcpTopicsConfig {
    @Value("${gcp.project-id}")
    private String gcpProjectId;


    @Value("${gcp.pubsub.transaction.initiated.topic}")
    private String transactionInitiatedTopic;

    @Value("${gcp.pubsub.transaction.completed.topic}")
    private String transactionCompletedTopic;

    @Value("${gcp.pubsub.transaction.refunded.topic}")
    private String transactionRefundedTopic;

    @Value("${gcp.pubsub.fraud.detected.topic}")
    private String fraudDetectedTopic;

    @Value("${gcp.pubsub.transaction.otp.generated.topic}")
    private String transactionOtpGeneratedTopic;


    @Bean(name = "transactionInitiatedTopicName")
    public TopicName transactionInitiatedTopicName() {
        return TopicName.of(gcpProjectId, transactionInitiatedTopic);
    }

    @Bean(name = "transactionCompletedTopicName")
    public TopicName transactionCompletedTopicName() {
        return TopicName.of(gcpProjectId, transactionCompletedTopic);
    }

    @Bean(name = "transactionRefundedTopicName")
    public TopicName transactionRefundedTopicName() {
        return TopicName.of(gcpProjectId, transactionRefundedTopic);
    }

    @Bean(name = "fraudDetectedTopicName")
    public TopicName fraudDetectedTopicName() {
        return TopicName.of(gcpProjectId, fraudDetectedTopic);
    }

    @Bean(name = "transactionOtpGeneratedTopicName")
    public TopicName transactionOtpGeneratedTopicName() {
        return TopicName.of(gcpProjectId, transactionOtpGeneratedTopic);
    }
}


