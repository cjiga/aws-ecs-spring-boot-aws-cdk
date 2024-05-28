package com.cjiga.auditservice.events.dto;

public record SnsAttributes(
        SnsMessageAttribute traceId,
        SnsMessageAttribute eventType,
        SnsMessageAttribute requestId
) {
}
