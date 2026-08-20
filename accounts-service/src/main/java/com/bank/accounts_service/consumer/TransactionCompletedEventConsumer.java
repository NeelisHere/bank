package com.bank.accounts_service.consumer;

import com.bank.accounts_service.service.AccountBalanceService;
import com.bank.common_lib.events.TransactionEvent;
import com.bank.common_lib.exception.AccountNotFoundException;
import com.bank.common_lib.exception.CommonException;
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
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransactionCompletedEventConsumer {
    private final Subscriber subscriber;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AccountBalanceService accountBalanceService;

    public TransactionCompletedEventConsumer(
            @Qualifier("transactionCompletedSubscriptionName")
            ProjectSubscriptionName projectSubscriptionName,
            AccountBalanceService accountBalanceService
    ) {
        this.accountBalanceService = accountBalanceService;
        MessageReceiver receiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
            try {
                TransactionEvent transactionEvent = this.objectMapper.readValue(message.getData().toStringUtf8(), TransactionEvent.class);
                log.info("transaction event received: {}", transactionEvent);
                log.info("Crediting amount={} to account={}", transactionEvent.amount(), transactionEvent.receiverAccountNumber());
                this.accountBalanceService.creditBalance(transactionEvent.receiverAccountNumber(), transactionEvent.amount());
                consumer.ack();
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize TransactionEvent: {}", e.getMessage());
                consumer.ack(); // malformed message — retrying will never fix it
            } catch (CommonException | AccountNotFoundException e) {
                log.error("Business error processing transaction, discarding message: {}", e.getMessage());
                consumer.ack(); // permanent business failure — retrying will never fix it
            } catch (Exception e) {
                log.error("Transient error processing transaction, will retry: {}", e.getMessage());
                consumer.nack(); // transient failure (e.g. GCP down) — retry is appropriate
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
