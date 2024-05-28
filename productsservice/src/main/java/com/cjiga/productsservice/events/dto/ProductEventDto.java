package com.cjiga.productsservice.events.dto;

public record ProductEventDto(
        String id,
        String code,
        String email,
        float price
) {
}
