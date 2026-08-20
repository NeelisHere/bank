package com.bank.payment_service.controller;

import com.bank.common_lib.dto.request.CreatePaymentRequest;
import com.bank.common_lib.dto.response.PaymentOrderResponse;
import com.bank.payment_service.service.PaymentService;
import com.bank.payment_service.service.RazorpayService;
import com.razorpay.RazorpayException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(path = "/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final RazorpayService razorpayService;

    @PostMapping("/create-order")
    public ResponseEntity<PaymentOrderResponse> createPaymentOrder(@Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPaymentOrder(request));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> payload) {
        razorpayService.handleRazorpayWebhook(payload);
        return ResponseEntity.status(HttpStatus.OK).body("Webhook has been processed successfully!");
    }
}
