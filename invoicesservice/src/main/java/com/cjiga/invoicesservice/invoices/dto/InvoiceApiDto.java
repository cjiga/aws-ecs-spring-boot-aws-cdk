package com.cjiga.invoicesservice.invoices.dto;

import java.util.List;

public record InvoiceApiDto(
        String email,
        String invoiceNumber,
        Float totalValue,
        List<InvoiceProductApiDto> products,
        String invoiceTransactionId,
        String fileTransactionId,
        Long createdAt
) {
}
