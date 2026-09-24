package com.ecommerce.payment.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecommerce.payment.service.PaymentProcessor;

@Component
public class PaymentConsumer {

    private final PaymentProcessor paymentProcessor;

    public PaymentConsumer(
            PaymentProcessor paymentProcessor) {

        this.paymentProcessor = paymentProcessor;
    }

    @KafkaListener(
            topics = "order-created",
            groupId = "payment-service-group")
    public void consume(String payload) throws Exception {

        paymentProcessor.process(payload);
    }
}