package com.bank.notification_service.consumer;

import com.bank.common_lib.events.OtpEvent;
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
public class TransactionOtpGeneratedEventConsumer {
    private final Subscriber subscriber;
    private final ObjectMapper objectMapper = new ObjectMapper();
    public TransactionOtpGeneratedEventConsumer(
            @Qualifier("transactionOtpGeneratedSubscriptionName") ProjectSubscriptionName projectSubscriptionName
    ) {
        MessageReceiver messageReceiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
            try {
                OtpEvent event = objectMapper.readValue(message.getData().toStringUtf8(), OtpEvent.class);
                log.info("Received Transaction OTP event: {}", event);
                SendAlertService.sendAlert(
                        event.accountNumber(),
                        "OTP: TRANSACTION VERIFICATION REQUIRED",
                        String.format("""
                        Suspicious activity detected on your account.
                        Reason: %s.
                        A transaction of ₹%s is pending verification.
                        Your OTP is: %s. Valid for 2 minutes.
                        If this wasn't you — ignore this message.
                        Transaction will be cancelled and amount refunded automatically.
                        """, event.reason(), event.amount(), event.otp())
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
