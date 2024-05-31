package com.cjiga.invoicesservice.invoices.dto;

public record InvoiceFileTransactionApiDto(
        String transactionId,
        String status
) {
}
