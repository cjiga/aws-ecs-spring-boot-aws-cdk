package com.cjiga.invoicesservice.invoices.repositories;

import com.amazonaws.xray.spring.aop.XRayEnabled;
import com.cjiga.invoicesservice.invoices.enums.InvoiceFileTransactionStatus;
import com.cjiga.invoicesservice.invoices.models.InvoiceFileTransaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@XRayEnabled
@Repository
public class InvoicesFileTransactionsRepository {
    private static final String PARTITION_KEY = "#fileTransaction";
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    private final DynamoDbAsyncTable<InvoiceFileTransaction> invoiceFileTransactionTable;

    public InvoicesFileTransactionsRepository(
            @Value("${invoices.ddb.name}") String invoicesDdbName,
            DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient) {
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        invoiceFileTransactionTable = dynamoDbEnhancedAsyncClient.table(invoicesDdbName,
                TableSchema.fromBean(InvoiceFileTransaction.class));
    }

    public CompletableFuture<Void> createInvoiceFileTransaction(
            String transactionId, String requestId, int expiredIn
    ) {
        long timestamp = Instant.now().toEpochMilli();;
        long ttl = Instant.now().plusSeconds(300).getEpochSecond();

        InvoiceFileTransaction invoiceFileTransaction = new InvoiceFileTransaction();
        invoiceFileTransaction.setPk(PARTITION_KEY);
        invoiceFileTransaction.setSk(transactionId);
        invoiceFileTransaction.setTtl(ttl);
        invoiceFileTransaction.setRequestId(requestId);
        invoiceFileTransaction.setCreatedAt(timestamp);
        invoiceFileTransaction.setExpiresIn(expiredIn);
        invoiceFileTransaction.setFileTransactionStatus(InvoiceFileTransactionStatus.GENERATED);

        return invoiceFileTransactionTable.putItem(invoiceFileTransaction);
    }

    public CompletableFuture<InvoiceFileTransaction> updateInvoiceFileTransactionStatus(
            String transactionId, InvoiceFileTransactionStatus status
    ) {
        InvoiceFileTransaction invoiceFileTransaction = new InvoiceFileTransaction();
        invoiceFileTransaction.setPk(PARTITION_KEY);
        invoiceFileTransaction.setSk(transactionId);
        invoiceFileTransaction.setFileTransactionStatus(status);

        return  invoiceFileTransactionTable.updateItem(UpdateItemEnhancedRequest.builder(InvoiceFileTransaction.class)
                        .item(invoiceFileTransaction)
                        .ignoreNulls(true)
                        .conditionExpression(Expression.builder()
                                .expression("attribute_exists(sk)")
                                .build())
                .build());
    }

    public CompletableFuture<InvoiceFileTransaction> getInvoiceFileTransaction(String transactionId) {
        return invoiceFileTransactionTable.getItem(GetItemEnhancedRequest.builder()
                        .key(Key.builder()
                                .partitionValue(PARTITION_KEY)
                                .sortValue(transactionId)
                                .build())
                .build());
    }
}
