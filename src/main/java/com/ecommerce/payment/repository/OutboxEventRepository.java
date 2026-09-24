package com.ecommerce.payment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecommerce.payment.entity.OutboxEvent;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, String> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(
            String status);

    @Modifying
    @Query("""
           update OutboxEvent e
           set e.status = 'PROCESSING'
           where e.eventId = :eventId
             and e.status = 'NEW'
           """)
    int claim(@Param("eventId") String eventId);
}