package com.bank.payment_service.config;

import com.google.pubsub.v1.TopicName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcpPublisherConfig {

    @Value("${gcp.project-id}")
    private String gcpProjectId;

    @Value("${gcp.pubsub.payment.completed.topic}")
    private String paymentCompletedTopic;

    @Value("${gcp.pubsub.payment.failed.topic}")
    private String paymentFailedTopic;

    @Bean(name = "paymentCompletedTopicName")
    public TopicName paymentCompletedTopicName() {
        return TopicName.of(gcpProjectId, paymentCompletedTopic);
    }

    @Bean(name = "paymentFailedTopicName")
    public TopicName paymentFailedTopicName() {
        return TopicName.of(gcpProjectId, paymentFailedTopic);
    }
}
