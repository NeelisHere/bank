package com.bank.notification_service.consumer;

import com.bank.common_lib.events.FraudDetectedEvent;
import com.bank.notification_service.service.SendAlertService;
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

@Slf4j
@Component
public class FraudDetectedEventConsumer {
    private final Subscriber subscriber;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FraudDetectedEventConsumer(
            @Qualifier("fraudDetectedSubscriptionName") ProjectSubscriptionName projectSubscriptionName
    ) {
        MessageReceiver messageReceiver = (message, consumer) -> {
            try {
                FraudDetectedEvent event = objectMapper.readValue(message.getData().toStringUtf8(), FraudDetectedEvent.class);
                log.info("Received fraud detected event: {}", event);
                SendAlertService.sendAlert(
                        event.accountNumber(),
                        "ACCOUNT BLOCKED",
                        String.format("""
                                Your account %s has been blocked!
                                Reason: %s
                                Please contact your bank immediately!        
                                """,
                                event.accountNumber(), event.reason()
                        )
                );
                consumer.ack();
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize FraudDetectedEvent: {}", e.getMessage());
                consumer.ack();
            } catch (Exception e) {
                log.error("Error processing FraudDetectedEvent: {}", e.getMessage());
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
