package com.bank.notification_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
public class SendAlertService {
    public static void sendAlert(String accountNumber, String subject, String message) {
        log.info("\naccount no: {}\nsub: {}\n{}", accountNumber, subject, message);
    }
}
