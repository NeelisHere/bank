package com.bank.accounts_service.consumer;

import com.bank.accounts_service.service.AccountStatusService;
import com.bank.common_lib.enums.AccountStatus;
import com.bank.common_lib.events.FraudDetectedEvent;
import com.bank.common_lib.exception.AccountNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FraudDetectedEventConsumer {
    private final Subscriber subscriber;
    private final AccountStatusService accountStatusService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FraudDetectedEventConsumer(
            @Qualifier("fraudDetectedSubscriptionName")
            ProjectSubscriptionName projectSubscriptionName, AccountStatusService accountStatusService
    ) {
        this.accountStatusService = accountStatusService;
        MessageReceiver receiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
            try {
                FraudDetectedEvent fraudDetectedEvent = this.objectMapper.readValue(message.getData().toStringUtf8(), FraudDetectedEvent.class);
                log.info("fraud event received: {}", fraudDetectedEvent);
                log.info("Fraud detected — blocking account: {}", fraudDetectedEvent.accountNumber());
                this.accountStatusService.blockAccount(fraudDetectedEvent.accountNumber());
                consumer.ack();
            } catch (AccountNotFoundException e) {
                log.error(e.getMessage());
                consumer.ack();
            }catch (Exception e) {
                log.error(e.getMessage());
                consumer.nack();
            }
        };
        this.subscriber = Subscriber.newBuilder(projectSubscriptionName, receiver).build();
    }

    @PostConstruct
    public void start() {
        subscriber.startAsync().awaitRunning();
    }

    @PreDestroy
    public void stop() {
        subscriber.stopAsync();
    }
}
