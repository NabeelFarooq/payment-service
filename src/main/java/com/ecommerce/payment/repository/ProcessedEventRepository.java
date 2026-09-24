package com.ecommerce.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.payment.entity.ProcessedEventEntity;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEventEntity, String> {
}