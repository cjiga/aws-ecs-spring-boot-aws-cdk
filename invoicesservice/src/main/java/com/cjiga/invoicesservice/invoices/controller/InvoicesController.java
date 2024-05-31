package com.cjiga.invoicesservice.invoices.controller;

import com.amazonaws.xray.spring.aop.XRayEnabled;
import com.cjiga.invoicesservice.invoices.dto.InvoiceApiDto;
import com.cjiga.invoicesservice.invoices.dto.InvoiceFileTransactionApiDto;
import com.cjiga.invoicesservice.invoices.dto.InvoiceProductApiDto;
import com.cjiga.invoicesservice.invoices.dto.UrlResponseDto;
import com.cjiga.invoicesservice.invoices.models.InvoiceFileTransaction;
import com.cjiga.invoicesservice.invoices.repositories.InvoicesFileTransactionsRepository;
import com.cjiga.invoicesservice.invoices.repositories.InvoicesRepository;
import com.cjiga.invoicesservice.invoices.services.S3InvoicesService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@XRayEnabled
@RestController
@RequestMapping("/api/invoices")
public class InvoicesController {

    private static final Logger LOG = LogManager.getLogger(InvoicesController.class);
    private final S3InvoicesService s3InvoicesService;
    private final InvoicesRepository invoicesRepository;
    private final InvoicesFileTransactionsRepository invoicesFileTransactionsRepository;

    @Autowired
    public InvoicesController(S3InvoicesService s3InvoicesService,
                              InvoicesRepository invoicesRepository,
                              InvoicesFileTransactionsRepository invoicesFileTransactionsRepository) {
        this.s3InvoicesService = s3InvoicesService;
        this.invoicesRepository = invoicesRepository;
        this.invoicesFileTransactionsRepository = invoicesFileTransactionsRepository;
    }

    @GetMapping()
    public ResponseEntity<List<InvoiceApiDto>> getAllInvoicesByEmail(@RequestParam() String email) {
        LOG.info("Get all invoices by email");
        List<InvoiceApiDto> invoicesApiDto = new ArrayList<>();
        invoicesRepository.findByCustomerEmail(email).subscribe(invoicePage -> {
            invoicesApiDto.addAll(
                    invoicePage.items().parallelStream()
                            .map(invoice -> new InvoiceApiDto(
                                    invoice.getPk().split("_")[1],
                                    invoice.getSk(),
                                    invoice.getTotalValue(),
                                    invoice.getProducts().parallelStream()
                                            .map(invoiceProduct -> new InvoiceProductApiDto(
                                                    invoiceProduct.getId(),
                                                    invoiceProduct.getQuantity()
                                            )).toList(),
                                    invoice.getInvoiceTransactionId(),
                                    invoice.getFileTransactionId(),
                                    invoice.getCreatedAt()
                            )).toList());
        }).join();

        return new ResponseEntity<>(invoicesApiDto, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<UrlResponseDto> generatePreSignedUrl(
            @RequestHeader("requestId") String requestId) {
        String key = UUID.randomUUID().toString();
        int expiresIn = 300;

        ThreadContext.put("invoiceFileTransactionId", key);

        String preSignedUrl = s3InvoicesService.generatePreSignedUrl(key, expiresIn);

        invoicesFileTransactionsRepository.createInvoiceFileTransaction(key, requestId, expiresIn).join();

        LOG.info("Invoice file transaction generated...");

        return new ResponseEntity<>(new UrlResponseDto(preSignedUrl, expiresIn, key), HttpStatus.OK);
    }

    @GetMapping("/transactions/{fileTransactionId}")
    public ResponseEntity<?> getInvoiceFileTransaction(@PathVariable("fileTransactionId") String fileTransactionId) {
        LOG.info("Get invoice file transaction by this id {}", fileTransactionId);
        InvoiceFileTransaction invoiceFileTransaction = invoicesFileTransactionsRepository
                .getInvoiceFileTransaction(fileTransactionId).join();

        if (invoiceFileTransaction != null) {
            return new ResponseEntity<>(new InvoiceFileTransactionApiDto(
                    fileTransactionId, invoiceFileTransaction.getFileTransactionStatus().name()
            ),HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Invoice file transaction not found", HttpStatus.NOT_FOUND);
        }
    }
}
