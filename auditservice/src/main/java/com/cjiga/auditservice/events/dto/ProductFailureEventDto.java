package com.cjiga.auditservice.events.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductFailureEventDto(
        String email,
        int status,
        String error,
        String id
) {
}
