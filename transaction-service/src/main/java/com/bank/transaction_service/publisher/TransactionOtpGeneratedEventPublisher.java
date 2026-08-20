package com.bank.transaction_service.publisher;

import com.bank.common_lib.events.OtpEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TransactionOtpGeneratedEventPublisher {
    private final Publisher publisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TransactionOtpGeneratedEventPublisher(
            @Qualifier("transactionOtpGeneratedTopicName") TopicName topicName
    ) throws IOException {
        this.publisher = Publisher.newBuilder(topicName).build();
    }

    public void publish(OtpEvent event) {
        try {
            PubsubMessage message = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFromUtf8(objectMapper.writeValueAsString(event)))
                    .build();
            ApiFuture<String> future = publisher.publish(message);
            log.info("Published OTP generated event for transaction {}, messageId={}", event.transactionId(), future.get());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OtpEvent for transaction {}: {}", event.transactionId(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to publish OTP generated event for transaction {}: {}", event.transactionId(), e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Closing TransactionOtpGeneratedEventPublisher Pub/Sub resources...");
        try {
            publisher.shutdown();
            publisher.awaitTermination(5, TimeUnit.SECONDS);
            log.info("TransactionOtpGeneratedEventPublisher shutdown complete.");
        } catch (InterruptedException e) {
            log.error("TransactionOtpGeneratedEventPublisher shutdown interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
}
