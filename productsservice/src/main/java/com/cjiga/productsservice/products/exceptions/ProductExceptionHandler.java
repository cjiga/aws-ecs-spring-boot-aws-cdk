package com.cjiga.productsservice.products.exceptions;

import com.cjiga.productsservice.events.dto.ProductFailureEventDto;
import com.cjiga.productsservice.events.services.EventsPublisher;
import com.cjiga.productsservice.products.dto.ProductErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import software.amazon.awssdk.services.sns.model.PublishResponse;

@RestControllerAdvice
public class ProductExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LogManager.getLogger(ProductExceptionHandler.class);
    private final EventsPublisher eventsPublisher;

    @Autowired
    public ProductExceptionHandler(EventsPublisher eventsPublisher) {
        this.eventsPublisher = eventsPublisher;
    }

    @ExceptionHandler(value = { ProductException.class})
    protected ResponseEntity<Object> handleProductError(ProductException productException, WebRequest webRequest)
            throws JsonProcessingException {
        ProductErrorResponse productErrorResponse = new ProductErrorResponse(
                productException.getProductsErrors().getMessage(),
                productException.getProductsErrors().getHttpStatus().value(),
                ThreadContext.get("requestId"),
                productException.getProductId()
        );

        ProductFailureEventDto productFailureEventDto = new ProductFailureEventDto(
                "c.jiga1983@gmail.com",
                productException.getProductsErrors().getHttpStatus().value(),
                productException.getProductsErrors().getMessage(),
                productException.getProductId()

        );

        PublishResponse publishResponse = eventsPublisher.sendProductFailureEvent(productFailureEventDto).join();
        ThreadContext.put("messageId", publishResponse.messageId());

        LOG.error(productException.getProductsErrors().getMessage());

        return handleExceptionInternal(
                productException,
                productErrorResponse,
                new HttpHeaders(),
                productException.getProductsErrors().getHttpStatus(),
                webRequest
        );
    }
}
