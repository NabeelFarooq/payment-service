package com.ecommerce.payment.service;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.payment.entity.OutboxEvent;

import com.ecommerce.payment.repository.OutboxEventRepository;

@Component
public class OutboxPublisher {

    private final OutboxEventRepository repository;

    private final KafkaTemplate<String, String>
            kafkaTemplate;

    public OutboxPublisher(
            OutboxEventRepository repository,
            KafkaTemplate<String, String> kafkaTemplate) {

        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    @Scheduled(fixedDelay = 5000)
    public void publishEvents() {

        List<OutboxEvent> events =
                repository
                        .findTop100ByStatusOrderByCreatedAtAsc(
                                "NEW");

        for (OutboxEvent event : events) {

            try {
                publishOne(event);

            } catch (Exception ex) {

                System.err.println(
                        "Payment outbox failed: "
                        + event.getEventId()
                        + " - "
                        + ex.getMessage());
            }
        }
    }

    
    protected void publishOne(
            OutboxEvent event) throws Exception {

        if (repository.claim(
                event.getEventId()) != 1) {

            return;
        }

        try {

            kafkaTemplate
                    .send(
                            event.getTopic(),
                            event.getMessageKey(),
                            event.getPayload())
                    .get();

            event.setStatus("PUBLISHED");

            repository.save(event);

        } catch (Exception ex) {

            event.setStatus("NEW");

            repository.save(event);

            throw ex;
        }
    }
}