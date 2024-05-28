package com.cjiga.auditservice.products.dto;

import com.cjiga.auditservice.products.models.ProductEvent;

public record ProductEventApiDto(
        String productId,
        String code,
        float price,
        String requestId,
        String email,
        long createdAt) {
    public ProductEventApiDto(ProductEvent productEvent) {
        this(
                productEvent.getInfo().getId(),
                productEvent.getInfo().getCode(),
                productEvent.getInfo().getPrice(),
                productEvent.getInfo().getRequestId(),
                productEvent.getEmail(),
                productEvent.getCreatedAt()
        );
    }
}
