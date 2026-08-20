package com.bank.payment_service.publisher;

import com.bank.common_lib.events.PaymentFailedEvent;
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
public class PaymentFailedPublisher {

    private final Publisher publisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentFailedPublisher(@Qualifier("paymentFailedTopicName") TopicName topicName) throws IOException {
        this.publisher = Publisher.newBuilder(topicName).build();
    }

    public void publish(PaymentFailedEvent event) {
        try {
            PubsubMessage message = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFromUtf8(objectMapper.writeValueAsString(event)))
                    .build();
            ApiFuture<String> future = publisher.publish(message);
            log.info("Published payment.failed event for paymentId={}, messageId={}",
                    event.paymentId(), future.get());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PaymentFailedEvent for paymentId={}: {}",
                    event.paymentId(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to publish payment.failed event for paymentId={}: {}",
                    event.paymentId(), e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Closing PaymentFailedPublisher Pub/Sub resources...");
        try {
            publisher.shutdown();
            publisher.awaitTermination(5, TimeUnit.SECONDS);
            log.info("PaymentFailedPublisher shutdown complete.");
        } catch (InterruptedException e) {
            log.error("PaymentFailedPublisher shutdown interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
}
