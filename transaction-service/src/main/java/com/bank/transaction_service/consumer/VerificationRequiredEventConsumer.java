package com.bank.transaction_service.consumer;

import com.bank.common_lib.events.VerificationEvent;
import com.bank.common_lib.exception.CommonException;
import com.bank.common_lib.exception.TransactionNotFoundException;
import com.bank.transaction_service.service.TransactionVerificationService;
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
public class VerificationRequiredEventConsumer {
    private final TransactionVerificationService transactionVerificationService;
    private final Subscriber subscriber;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VerificationRequiredEventConsumer(
            TransactionVerificationService transactionOtpService,
            @Qualifier("verificationRequiredSubscription") ProjectSubscriptionName projectSubscriptionName
    ) {
        this.transactionVerificationService = transactionOtpService;
        MessageReceiver receiver = (message, ackReplyConsumer) -> {
            try {
                VerificationEvent event = objectMapper.readValue(message.getData().toStringUtf8(), VerificationEvent.class);
                log.info("Verification required event received: {}", event);
                transactionVerificationService.handleVerificationRequired(event);
                ackReplyConsumer.ack();
            } catch (JsonProcessingException | CommonException | TransactionNotFoundException e) {
                log.error(e.getMessage());
                ackReplyConsumer.ack();
            } catch (Exception e) {
                log.error("Error processing VerificationEvent: {}", e.getMessage());
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
