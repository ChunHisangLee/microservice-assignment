package com.jack.transactionservice.controller;


import com.jack.common.dto.request.CreateTransactionRequestDto;
import com.jack.transactionservice.dto.TransactionDto;
import com.jack.transactionservice.entity.TransactionType;
import com.jack.transactionservice.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.jack.common.constants.ApplicationConstants.DEFAULT_PAGE_NUMBER;
import static com.jack.common.constants.ApplicationConstants.DEFAULT_PAGE_SIZE;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/buy")
    public ResponseEntity<TransactionDto> buyBtc(@RequestBody CreateTransactionRequestDto request) {
        logger.info("Initiating BTC buy transaction for user ID: {}", request.getUserId());
        TransactionDto transactionDTO = transactionService.createTransaction(request, TransactionType.BUY);
        logger.info("BTC buy transaction completed for user ID: {}", request.getUserId());
        return ResponseEntity.ok(transactionDTO);
    }

    @PostMapping("/sell")
    public ResponseEntity<TransactionDto> sellBtc(@RequestBody CreateTransactionRequestDto request) {
        logger.info("Initiating BTC sell transaction for user ID: {}", request.getUserId());
        TransactionDto transactionDTO = transactionService.createTransaction(request, TransactionType.SELL);
        logger.info("BTC sell transaction completed for user ID: {}", request.getUserId());
        return ResponseEntity.ok(transactionDTO);
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<Page<TransactionDto>> getUserTransactionHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {

        logger.info("Fetching transaction history for user ID: {} with page number: {} and size: {}", userId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionDto> transactions = transactionService.getUserTransactionHistory(userId, pageable);
        logger.info("Fetched {} transactions for user ID: {}", transactions.getTotalElements(), userId);
        return ResponseEntity.ok(transactions);
    }
}
