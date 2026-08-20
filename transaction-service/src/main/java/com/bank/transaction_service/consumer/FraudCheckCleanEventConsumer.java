package com.bank.transaction_service.consumer;

import com.bank.common_lib.events.FraudCheckCleanEvent;
import com.bank.common_lib.exception.CommonException;
import com.bank.transaction_service.service.TransactionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Slf4j
@Component
public class FraudCheckCleanEventConsumer {
    private final TransactionService transactionService;
    private final Subscriber subscriber;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FraudCheckCleanEventConsumer(
            TransactionService transactionService,
            @Qualifier("fraudCheckCleanSubscription")
            ProjectSubscriptionName projectSubscriptionName
    ) {
        this.transactionService = transactionService;
        MessageReceiver receiver = (message, ackReplyConsumer) -> {
            try {
                FraudCheckCleanEvent fraudCheckCleanEvent = objectMapper.readValue(message.getData().toStringUtf8(), FraudCheckCleanEvent.class);
                log.info("fraudCheckCleanEvent received: {}", fraudCheckCleanEvent);
                UUID transactionId = UUID.fromString(fraudCheckCleanEvent.transactionId());
                transactionService.completeTransaction(transactionId);
                ackReplyConsumer.ack();
            } catch (JsonProcessingException | CommonException e) {
                log.error(e.getMessage());
                ackReplyConsumer.ack();
            } catch (Exception e) {
                log.error(e.getMessage());
                ackReplyConsumer.nack();
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
