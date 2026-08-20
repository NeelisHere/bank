package com.bank.fraud_service.publisher;

import com.bank.common_lib.events.TransactionEvent;
import com.bank.common_lib.events.VerificationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class VerificationRequiredEventPublisher {

    private final Publisher publisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VerificationRequiredEventPublisher(
            @Qualifier("verificationRequiredTopicName") TopicName topicName) throws IOException {
        this.publisher = Publisher.newBuilder(topicName).build();
    }

    public void publish(VerificationEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            PubsubMessage message = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFromUtf8(payload))
                    .build();
            ApiFuture<String> future = publisher.publish(message);
            log.info("Published verification.required event for transaction {}, messageId={}", event.transactionId(), future.get());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize TransactionEvent for transaction {}: {}", event.transactionId(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to publish verification.required event for transaction {}: {}", event.transactionId(), e.getMessage());
        }
    }
}
