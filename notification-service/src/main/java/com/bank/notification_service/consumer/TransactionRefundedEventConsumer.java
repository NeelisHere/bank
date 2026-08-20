package com.bank.notification_service.consumer;

import com.bank.common_lib.events.RefundEvent;
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
public class TransactionRefundedEventConsumer {
    private final Subscriber subscriber;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TransactionRefundedEventConsumer(
            @Qualifier("transactionRefundedSubscriptionName") ProjectSubscriptionName projectSubscriptionName
    ) {
        MessageReceiver messageReceiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
            try {
                log.info("Received transaction refunded message: {}", message);
                RefundEvent event = objectMapper.readValue(message.getData().toStringUtf8(), RefundEvent.class);
                SendAlertService.sendAlert(
                        event.senderAccountNumber(),
                        "REFUND ALERT",
                        String.format(
                                "Your transaction of ₹%s was cancelled. ₹%s refunded to account %s",
                                event.amount(),
                                event.amount(),
                                event.senderAccountNumber()
                        )
                );
                consumer.ack();
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize RefundEvent: {}", e.getMessage());
                consumer.ack(); // malformed message — retrying will never fix it
            } catch (Exception e) {
                log.error("Error processing RefundEvent: {}", e.getMessage());
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
