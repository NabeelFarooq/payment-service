package com.ecommerce.payment.util;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventIdGenerator {

    private static final long CUSTOM_EPOCH = 1704067200000L;

    private static final int SEQUENCE_BITS = 12;
    private static final int WORKER_ID_BITS = 10;

    private static final long MAX_SEQUENCE =
            (1L << SEQUENCE_BITS) - 1;

    private static final long MAX_WORKER_ID =
            (1L << WORKER_ID_BITS) - 1;

    private final long workerId;

    private long lastTimestamp = -1L;

    private long sequence = 0L;

    public PaymentEventIdGenerator(
            @Value("${event.worker-id:1}") long workerId) {

        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    "event.worker-id out of range");
        }

        this.workerId = workerId;
    }

    public synchronized String generate() {

        long timestamp = Instant.now().toEpochMilli();

        if (timestamp < lastTimestamp) {
            throw new IllegalStateException(
                    "System clock moved backwards");
        }

        if (timestamp == lastTimestamp) {

            sequence = (sequence + 1) & MAX_SEQUENCE;

            if (sequence == 0) {
                timestamp = waitForNextMillis(timestamp);
            }

        } else {
            sequence = 0;
        }

        lastTimestamp = timestamp;

        long numeric =
                ((timestamp - CUSTOM_EPOCH)
                        << (WORKER_ID_BITS + SEQUENCE_BITS))
                | (workerId << SEQUENCE_BITS)
                | sequence;

        return "PAY-" + numeric;
    }

    private long waitForNextMillis(long timestamp) {

        long current = Instant.now().toEpochMilli();

        while (current <= timestamp) {
            current = Instant.now().toEpochMilli();
        }

        return current;
    }
}