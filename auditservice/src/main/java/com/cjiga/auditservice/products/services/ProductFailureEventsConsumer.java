package com.cjiga.auditservice.products.services;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.TraceID;
import com.cjiga.auditservice.events.dto.ProductEventDto;
import com.cjiga.auditservice.events.dto.ProductEventType;
import com.cjiga.auditservice.events.dto.ProductFailureEventDto;
import com.cjiga.auditservice.events.dto.SnsMessageDto;
import com.cjiga.auditservice.products.repositories.ProductFailureEventsRepositories;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@Service
public class ProductFailureEventsConsumer {

    private static final Logger LOG = LogManager.getLogger(ProductFailureEventsConsumer.class);
    private final ObjectMapper objectMapper;
    private final SqsAsyncClient sqsAsyncClient;
    private final String productFailureEventsQueueUrl;
    private final ReceiveMessageRequest receiveMessageRequest;
    private final ProductFailureEventsRepositories productFailureEventsRepository;

    @Autowired
    public ProductFailureEventsConsumer(ObjectMapper objectMapper,
                                        SqsAsyncClient sqsAsyncClient,
                                        @Value("${aws.sqs.queue.product.failure.events.url}")
                                            String productFailureEventsQueueUrl,
                                        ProductFailureEventsRepositories productFailureEventsRepository) {

        this.objectMapper = objectMapper;
        this.sqsAsyncClient = sqsAsyncClient;
        this.productFailureEventsQueueUrl = productFailureEventsQueueUrl;

        this.receiveMessageRequest = ReceiveMessageRequest.builder()
                .maxNumberOfMessages(10)
                .queueUrl(productFailureEventsQueueUrl)
                .build();
        this.productFailureEventsRepository = productFailureEventsRepository;
    }

    @Scheduled(fixedDelay=5000)
    public void receiveProductFailureEventsMessages() {
        List<Message> messages;
        while(!(messages = sqsAsyncClient.receiveMessage(receiveMessageRequest).join().messages()).isEmpty()) {
            LOG.info("Reading {} messages", messages.size());

            messages.parallelStream().forEach(message -> {
                SnsMessageDto snsMessageDto;
                try {
                    snsMessageDto= objectMapper.readValue(message.body(), SnsMessageDto.class);
                } catch (JsonProcessingException e) {
                    throw  new RuntimeException(e);
                }
                String requestId = snsMessageDto.messageAttributes().requestId().value();
                String messageId = snsMessageDto.messageId();
                String traceId = snsMessageDto.messageAttributes().traceId().value();

                Segment segment = AWSXRay.beginSegment("product-failure-events-consumer");
                segment.setOrigin("AWS::ECS::Container");
                segment.setStartTime(Instant.now().getEpochSecond());
                segment.setTraceId(TraceID.fromString(traceId));
                segment.run(() -> {
                    try {
                        ThreadContext.put("messageId", messageId);
                        ThreadContext.put("requestId", requestId);
                        ProductEventType eventType = ProductEventType
                                .valueOf(snsMessageDto.messageAttributes().eventType().value());
                        CompletableFuture<Void> productFailureEventFuture;
                        if (ProductEventType.PRODUCT_FAILURE == eventType) {
                            ProductFailureEventDto productFailureEventDto =
                                    objectMapper.readValue(snsMessageDto.message(), ProductFailureEventDto.class);
                            productFailureEventFuture = productFailureEventsRepository.create(productFailureEventDto, eventType,
                                    messageId, requestId, traceId);

                            LOG.info("Product failure event: {} - Id: {}", eventType, productFailureEventDto.id());
                        } else {
                            LOG.error("Invalid product failure event: {}", eventType);
                            throw new Exception("Invalid product failure event");
                        }

                        CompletableFuture<DeleteMessageResponse> deleteMessageCompletableFuture =
                                sqsAsyncClient.deleteMessage(DeleteMessageRequest.builder()
                                        .queueUrl(productFailureEventsQueueUrl)
                                        .receiptHandle(message.receiptHandle())
                                        .build());

                        CompletableFuture.allOf(productFailureEventFuture, deleteMessageCompletableFuture).join();

                        LOG.info("Message deleted...");
                    } catch (Exception e) {
                        LOG.error("Failed to parse product failure event message");
                        throw new RuntimeException(e);
                    } finally {
                        ThreadContext.clearAll();
                        segment.setEndTime(Instant.now().getEpochSecond());
                        segment.end();
                        segment.close();
                    }
                }, AWSXRay.getGlobalRecorder());
            });
        }
        AWSXRay.endSegment();
    }
}
