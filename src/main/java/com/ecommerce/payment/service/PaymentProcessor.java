package com.ecommerce.payment.service;

import java.time.LocalDateTime;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import com.ecommerce.payment.entity.OutboxEvent;
import com.ecommerce.payment.entity.PaymentEntity;
import com.ecommerce.payment.entity.ProcessedEventEntity;

import com.ecommerce.payment.event.OrderCreatedEvent;
import com.ecommerce.payment.event.PaymentEvent;

import com.ecommerce.payment.repository.OutboxEventRepository;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.repository.ProcessedEventRepository;

import com.ecommerce.payment.util.PaymentEventIdGenerator;

@Service
public class PaymentProcessor {

    private final ObjectMapper objectMapper;

    private final PaymentRepository paymentRepository;

    private final ProcessedEventRepository processedEventRepository;

    private final OutboxEventRepository outboxEventRepository;

    private final PaymentEventIdGenerator eventIdGenerator;

    private final int failureCustomerId;

    public PaymentProcessor(
            ObjectMapper objectMapper,
            PaymentRepository paymentRepository,
            ProcessedEventRepository processedEventRepository,
            OutboxEventRepository outboxEventRepository,
            PaymentEventIdGenerator eventIdGenerator,
            @Value("${payment.demo.failure-customer-id:102}")
            int failureCustomerId) {

        this.objectMapper = objectMapper;
        this.paymentRepository = paymentRepository;
        this.processedEventRepository =
                processedEventRepository;
        this.outboxEventRepository =
                outboxEventRepository;
        this.eventIdGenerator =
                eventIdGenerator;
        this.failureCustomerId =
                failureCustomerId;
    }

    @Transactional
    public void process(String payload) throws Exception {

        OrderCreatedEvent order =
                objectMapper.readValue(
                        payload,
                        OrderCreatedEvent.class);

        if (order.getEventId() == null
                || order.getEventId().isBlank()) {

            throw new IllegalArgumentException(
                    "Missing eventId");
        }

        if (order.getOrderId() == null
                || order.getOrderId().isBlank()) {

            throw new IllegalArgumentException(
                    "Missing orderId");
        }

        // Duplicate check
        if (processedEventRepository
                .existsById(order.getEventId())) {

            System.out.println(
                    "Duplicate event ignored: "
                    + order.getEventId());

            return;
        }

        // Used only to demonstrate DLT
        if (order.getOrderId().startsWith("DLT-")) {

            throw new IllegalStateException(
                    "Deliberate DLT test failure");
        }

        boolean success =
                order.getCustomerId() != failureCustomerId;

        PaymentEntity payment =
                new PaymentEntity();

        payment.setOrderId(
                order.getOrderId());

        payment.setCustomerId(
                order.getCustomerId());

        payment.setAmount(
                order.getAmount());

        payment.setPaymentMethod("UPI");

        payment.setPaymentStatus(
                success
                        ? "SUCCESS"
                        : "FAILED");

        payment.setReason(
                success
                        ? null
                        : "INSUFFICIENT_FUNDS");

        payment.setCreatedAt(
                LocalDateTime.now());

        payment.setPaymentId(
                eventIdGenerator
                        .generate()
                        .replace("PAY-", "TXN-"));

        paymentRepository.save(payment);

        // Create payment event
        PaymentEvent event =
                new PaymentEvent();

        event.setEventId(
                eventIdGenerator.generate());

        event.setEventType(
                success
                        ? "PAYMENT_SUCCESS"
                        : "PAYMENT_FAILED");

        event.setOrderId(
                order.getOrderId());

        event.setCustomerId(
                order.getCustomerId());

        event.setAmount(
                order.getAmount());

        event.setPaymentId(
                payment.getPaymentId());

        event.setPaymentMethod(
                payment.getPaymentMethod());

        event.setPaymentStatus(
                payment.getPaymentStatus());

        event.setReason(
                payment.getReason());

        event.setDeliveryAddress(
                order.getDeliveryAddress());

        event.setEventTime(
                LocalDateTime.now());

        // Outbox
        OutboxEvent outbox =
                new OutboxEvent();

        outbox.setEventId(
                event.getEventId());

        outbox.setEventType(
                event.getEventType());

        outbox.setTopic(
                success
                        ? "payment-success"
                        : "payment-failed");

        // IMPORTANT: Kafka key = orderId
        outbox.setMessageKey(
                event.getOrderId());

        outbox.setPayload(
                objectMapper.writeValueAsString(event));

        outbox.setStatus("NEW");

        outbox.setCreatedAt(
                LocalDateTime.now());

        outboxEventRepository.save(outbox);

        // Save processed event
        processedEventRepository.save(
                new ProcessedEventEntity(
                        order.getEventId(),
                        LocalDateTime.now()));
    }
}