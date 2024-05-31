package com.cjiga.invoicesservice.invoices.repositories;

import com.amazonaws.xray.spring.aop.XRayEnabled;
import com.cjiga.invoicesservice.invoices.enums.InvoiceTransactionStatus;
import com.cjiga.invoicesservice.invoices.models.InvoiceTransaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@XRayEnabled
@Repository
public class InvoiceTransactionsRepository {

    private static final String PARTITION_KEY = "#invoiceTransaction_";
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    private final DynamoDbAsyncTable<InvoiceTransaction> invoiceTransactionTable;

    public InvoiceTransactionsRepository(
            @Value("${invoices.ddb.name}") String invoicesDdbName,
            DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient) {
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        invoiceTransactionTable = dynamoDbEnhancedAsyncClient.table(invoicesDdbName,
                TableSchema.fromBean(InvoiceTransaction.class));
    }

    public CompletableFuture<Void> createInvoiceTransaction(String customerEmail, String invoiceNumber,
                                                            String invoiceTransactionId,
                                                            String invoiceFileTransactionId,
                                                            InvoiceTransactionStatus invoiceTransactionStatus) {
        long timestamp = Instant.now().toEpochMilli();;
        long ttl = Instant.now().plusSeconds(300).getEpochSecond();

        InvoiceTransaction invoiceTransaction = new InvoiceTransaction();
        invoiceTransaction.setPk(PARTITION_KEY.concat(invoiceFileTransactionId)); // #invoiceTransaction_fileTransactionId
        invoiceTransaction.setSk(invoiceTransactionId);
        invoiceTransaction.setTtl(ttl);
        invoiceTransaction.setCreatedAt(timestamp);
        invoiceTransaction.setCustomerEmail(customerEmail);
        invoiceTransaction.setInvoiceNumber(invoiceNumber);
        invoiceTransaction.setTransactionStatus(invoiceTransactionStatus);


        return  invoiceTransactionTable.putItem(invoiceTransaction);
    }

}
