package com.cjiga.auditservice.products.repositories;

import com.cjiga.auditservice.events.dto.ProductFailureEventDto;
import com.cjiga.auditservice.products.models.ProductInfoFailureEvent;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import com.cjiga.auditservice.events.dto.ProductEventDto;
import com.cjiga.auditservice.events.dto.ProductEventType;
import com.cjiga.auditservice.products.models.ProductEvent;
import com.cjiga.auditservice.products.models.ProductFailureEvent;
import com.cjiga.auditservice.products.models.ProductInfoEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Repository
public class ProductFailureEventsRepositories {

    private static final Logger LOG = LogManager.getLogger(ProductEventsRepository.class);
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    private final DynamoDbAsyncTable<ProductFailureEvent> productFailureEventTable;

    @Autowired
    public ProductFailureEventsRepositories(@Value("${aws.events.ddb}") String eventsDdbName,
                                   DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient) {
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.productFailureEventTable = dynamoDbEnhancedAsyncClient.table(eventsDdbName,
                TableSchema.fromBean(ProductFailureEvent.class));
    }

    public CompletableFuture<Void> create(ProductFailureEventDto productFailureEventDto,
                                          ProductEventType productEventType,
                                          String messageId, String requestId, String traceId) {
        long timestamp = Instant.now().toEpochMilli();
        long ttl = Instant.now().plusSeconds(300).getEpochSecond();

        ProductFailureEvent productFailureEvent =  new ProductFailureEvent();
        productFailureEvent.setPk("#product_".concat(productEventType.name()));
        productFailureEvent.setSk(String.valueOf(timestamp));
        productFailureEvent.setCreatedAt(timestamp);
        productFailureEvent.setTtl(ttl);
        productFailureEvent.setEmail(productFailureEventDto.email());

        ProductInfoFailureEvent productInfoFailureEvent = new ProductInfoFailureEvent();
        productInfoFailureEvent.setId(productFailureEventDto.id());
        productInfoFailureEvent.setMessageId(messageId);
        productInfoFailureEvent.setRequestId(requestId);
        productInfoFailureEvent.setTraceId(traceId);
        productInfoFailureEvent.setError(productFailureEventDto.error());
        productInfoFailureEvent.setStatus(productFailureEventDto.status());

        productFailureEvent.setInfo(productInfoFailureEvent);
        return productFailureEventTable.putItem(productFailureEvent);
    }
}
