package com.bank.payment_service.service;

import com.bank.common_lib.dto.request.CreatePaymentRequest;
import com.bank.common_lib.dto.response.PaymentOrderResponse;
import com.bank.common_lib.enums.PaymentStatus;
import com.bank.payment_service.repository.PaymentRepository;
import com.bank.payment_service.mapper.PaymentMapper;
import com.bank.payment_service.model.Payment;
import com.razorpay.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    @Value("${razorpay.access.key}")
    private String razorpayAccessKey;

    private final RazorpayService razorpayService;
    private final PaymentRepository paymentRepository;

    public PaymentOrderResponse createPaymentOrder(CreatePaymentRequest request) {
        log.info("creating payment order for request: {}", request);
        Order razorpayOrder = razorpayService.createRazorpayOrder(request);

        Payment payment = new Payment();
        payment.setRazorpayOrderId(razorpayOrder.get("id").toString());
        payment.setAccountNumber(request.accountNumber());
        payment.setAmount(request.amount());
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setDescription(request.description());

        Payment savedPayment =  paymentRepository.save(payment);
        return PaymentMapper.toResponse(savedPayment, razorpayAccessKey);
    }
}
