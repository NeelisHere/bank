package com.bank.transaction_service.publisher;

import com.bank.common_lib.events.TransactionEvent;
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
public class TransactionCompletedEventPublisher {
    private final Publisher publisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TransactionCompletedEventPublisher(@Qualifier("transactionCompletedTopicName") TopicName topicName) {
        try {
            this.publisher = Publisher.newBuilder(topicName).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void publish(TransactionEvent transactionEvent) {
        try {
            PubsubMessage message = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFromUtf8(objectMapper.writeValueAsString(transactionEvent)))
                    .build();
            ApiFuture<String> future = publisher.publish(message);
            log.info("Published transaction.completed event for transaction {}, messageId={}",
                    transactionEvent.transactionId(), future.get());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize TransactionEvent for transaction {}: {}", transactionEvent.transactionId(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to publish transaction.completed event for transaction {}: {}", transactionEvent.transactionId(), e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Closing Pub/Sub publisher resources...");
        try {
            publisher.shutdown();
            // Best Practice: Block for up to 5 seconds to let pending async messages flush out
            publisher.awaitTermination(5, TimeUnit.SECONDS);
            log.info("Pub/Sub publisher shutdown complete.");
        } catch (InterruptedException e) {
            log.error("Pub/Sub publisher shutdown sequence interrupted", e);
            Thread.currentThread().interrupt(); // Reset the thread interruption flag
        }
    }
}
