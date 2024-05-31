package com.cjiga.invoicesservice.invoices.dto;

public record UrlResponseDto(
        String url,
        int expireIn,
        String transactionId
) {
}
