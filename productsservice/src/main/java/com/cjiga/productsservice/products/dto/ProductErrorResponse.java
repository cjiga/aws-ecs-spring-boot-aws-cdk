package com.cjiga.productsservice.products.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ProductErrorResponse (
        String message,
        int statusCode,
        String requestId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String productId
){
}
