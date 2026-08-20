package com.bank.notification_service.consumer;

import com.bank.common_lib.events.TransactionEvent;
import com.bank.notification_service.service.SendAlertService;
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
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class TransactionCompletedEventConsumer {
    private final Subscriber subscriber;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TransactionCompletedEventConsumer(
            @Qualifier("transactionCompletedSubscriptionName") ProjectSubscriptionName projectSubscriptionName
    ) {
        MessageReceiver messageReceiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
            try {
                log.info("Received transaction completed message: {}", message);
                TransactionEvent event = objectMapper.readValue(message.getData().toStringUtf8(), TransactionEvent.class);
                SendAlertService.sendAlert(
                        event.senderAccountNumber(),
                        "DEBIT ALERT",
                        String.format("₹%s debited from account %s", event.amount(), event.senderAccountNumber())
                );
                SendAlertService.sendAlert(
                        event.receiverAccountNumber(),
                        "CREDIT ALERT",
                        String.format("₹%s credited to account %s", event.amount(), event.receiverAccountNumber())
                );
                consumer.ack();
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize TransactionEvent: {}", e.getMessage());
                consumer.ack();
            } catch (Exception e) {
                log.error("Error processing TransactionEvent: {}", e.getMessage());
                consumer.nack();
            }
        };
        this.subscriber = Subscriber.newBuilder(projectSubscriptionName, messageReceiver).build();
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
