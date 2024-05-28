package com.cjiga.productsservice.products.enums;

import org.springframework.http.HttpStatus;

public enum ProductsErrors {

    PRODUCT_NON_FOUND("Product not found", HttpStatus.NOT_FOUND),
    PRODUCT_CODE_ALREADY_EXISTS("Product code already exists", HttpStatus.CONFLICT);

    private final String message;
    private final HttpStatus httpStatus;

    ProductsErrors(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
