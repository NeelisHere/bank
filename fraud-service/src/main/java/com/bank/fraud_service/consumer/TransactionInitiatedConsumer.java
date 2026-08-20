package com.bank.fraud_service.consumer;

import com.bank.common_lib.events.TransactionEvent;
import com.bank.fraud_service.service.FraudService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransactionInitiatedConsumer {
    private final FraudService fraudService;
    private final Subscriber subscriber;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TransactionInitiatedConsumer(
            FraudService fraudService,
            @Qualifier("transactionInitiatedSubscriptionName") ProjectSubscriptionName subscriptionName
    ) {
        this.fraudService = fraudService;
        MessageReceiver receiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
            try {
                TransactionEvent event = objectMapper.readValue(message.getData().toStringUtf8(), TransactionEvent.class);
                log.info("Received transaction initiated event: {}", event);
                this.fraudService.checkTransaction(event);
                consumer.ack();
            } catch (Exception e) {
                log.error(e.getMessage());
                consumer.nack();
            }
        };
        this.subscriber = Subscriber.newBuilder(subscriptionName, receiver).build();
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
