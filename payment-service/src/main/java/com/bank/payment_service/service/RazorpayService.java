package com.bank.payment_service.service;

import com.bank.common_lib.dto.request.CreatePaymentRequest;
import com.bank.common_lib.enums.PaymentStatus;
import com.bank.common_lib.events.PaymentCompletedEvent;
import com.bank.common_lib.events.PaymentFailedEvent;
import com.bank.common_lib.exception.CommonException;
import com.bank.payment_service.mapper.PaymentEventMapper;
import com.bank.payment_service.model.Payment;
import com.bank.payment_service.publisher.PaymentCompletedPublisher;
import com.bank.payment_service.publisher.PaymentFailedPublisher;
import com.bank.payment_service.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.lang.reflect.MalformedParameterizedTypeException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayService {
    @Value("${razorpay.access.key}")
    private String razorpayAccessKey;

    @Value("${razorpay.secret.key}")
    private String razorpaySecretKey;

    private final PaymentRepository paymentRepository;

    private final PaymentCompletedPublisher paymentCompletedPublisher;
    private final PaymentFailedPublisher paymentFailedPublisher;

    private static final String PAYMENT_CAPTURED_STATUS = "payment.captured";
    private static final String PAYMENT_FAILED_STATUS = "payment.failed";

    public Order createRazorpayOrder(CreatePaymentRequest request) {
        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayAccessKey, razorpaySecretKey);
            int amountInPaise = request.amount().multiply(BigDecimal.valueOf(100)).intValue();
            JSONObject jsonOrderRequest = new JSONObject();
            jsonOrderRequest.put("amount", amountInPaise);
            jsonOrderRequest.put("currency", "INR");
            jsonOrderRequest.put("receipt",
                    String.format("rcpt_%s", UUID.randomUUID().toString().replace("-", "").substring(0, 30))
            );
            Order razorpayOrder = razorpayClient.orders.create(jsonOrderRequest);
            log.info("razorpay order created: {}", razorpayOrder);
            return razorpayOrder;
        } catch (RazorpayException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleRazorpayWebhook(Map<String, Object> payload) {
        log.info("Received Razorpay webhook: {}", payload);
        String event = (String) payload.get("event");
        if (PAYMENT_CAPTURED_STATUS.equals(event)) {
            handlePaymentSuccess(payload);
        } else if (PAYMENT_FAILED_STATUS.equals(event)) {
            handlePaymentFailure(payload);
        }
    }

    private void handlePaymentSuccess(Map<String, Object> payload) {
        try {
            Map<String, Object> paymentData = extractPaymentData(payload);
            String razorpayOrderId = (String) paymentData.get("order_id");
            String razorpayPaymentId = (String) paymentData.get("id");

            Payment payment = findByRazorpayOrderIdOrThrow(razorpayOrderId);
            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setStatus(PaymentStatus.COMPLETED);
            Payment savedPayment = paymentRepository.save(payment);

            PaymentCompletedEvent paymentCompletedEvent = PaymentEventMapper.toCompletedEvent(savedPayment);
            paymentCompletedPublisher.publish(paymentCompletedEvent);
        } catch (Exception e) {
            throw new CommonException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.name());
        }
    }

    private void handlePaymentFailure(Map<String, Object> payload) {
        try {
            Map<String, Object> paymentData = extractPaymentData(payload);
            String razorpayOrderId = (String) paymentData.get("order_id");

            Payment payment = findByRazorpayOrderIdOrThrow(razorpayOrderId);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment failed via Razorpay");
            Payment savedPayment = paymentRepository.save(payment);

            PaymentFailedEvent paymentFailedEvent = PaymentEventMapper.toFailedEvent(savedPayment);
            paymentFailedPublisher.publish(paymentFailedEvent);
        } catch (Exception e) {
            throw new CommonException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.name());
        }
    }

    private Payment findByRazorpayOrderIdOrThrow(String razorpayOrderId) {
        return paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new CommonException(
                    String.format("No records with razorpayOrderId: {} found!", razorpayOrderId),
                    HttpStatus.BAD_REQUEST.name()
                )
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPaymentData(Map<String, Object> payload) {
        payload = (Map<String, Object>) payload.get("payload");
        Map<String, Object> payment = (Map<String, Object>) payload.get("payment");
        return (Map<String, Object>) payment.get("entity");
    }
}
