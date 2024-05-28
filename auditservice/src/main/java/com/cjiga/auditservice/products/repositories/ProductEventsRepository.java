package com.cjiga.auditservice.products.repositories;

import com.cjiga.auditservice.events.dto.ProductEventDto;
import com.cjiga.auditservice.events.dto.ProductEventType;
import com.cjiga.auditservice.products.models.ProductEvent;
import com.cjiga.auditservice.products.models.ProductInfoEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;


@Repository
public class ProductEventsRepository {

    private static final Logger LOG = LogManager.getLogger(ProductEventsRepository.class);
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    private final DynamoDbAsyncTable<ProductEvent> productEventTable;

    @Autowired
    public ProductEventsRepository(@Value("${aws.events.ddb}") String eventsDdbName,
                                   DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient) {
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.productEventTable = dynamoDbEnhancedAsyncClient.table(eventsDdbName, TableSchema.fromBean(ProductEvent.class));
    }

    public CompletableFuture<Void> create(ProductEventDto productEventDto,
                                          ProductEventType productEventType,
                                          String messageId, String requestId, String traceId) {
        long timestamp = Instant.now().toEpochMilli();
        long ttl = Instant.now().plusSeconds(300).getEpochSecond();

        ProductEvent productEvent =  new ProductEvent();
        productEvent.setPk("#product_".concat(productEventType.name()));
        productEvent.setSk(String.valueOf(timestamp));
        productEvent.setCreatedAt(timestamp);
        productEvent.setTtl(ttl);
        productEvent.setEmail(productEventDto.email());

        ProductInfoEvent productInfoEvent = new ProductInfoEvent();
        productInfoEvent.setCode(productEventDto.code());
        productInfoEvent.setId(productEventDto.id());
        productInfoEvent.setPrice(productEventDto.price());
        productInfoEvent.setMessageId(messageId);
        productInfoEvent.setRequestId(requestId);
        productInfoEvent.setTraceId(traceId);

        productEvent.setInfo(productInfoEvent);
        return productEventTable.putItem(productEvent);
    }
}
