package com.jack.transactionservice.controller;


import com.jack.common.dto.request.CreateTransactionRequestDto;
import com.jack.transactionservice.dto.TransactionDto;
import com.jack.transactionservice.entity.TransactionType;
import com.jack.transactionservice.service.TransactionService;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.jack.common.constants.ApplicationConstants.DEFAULT_PAGE_NUMBER;
import static com.jack.common.constants.ApplicationConstants.DEFAULT_PAGE_SIZE;

@RestController
@RequestMapping("/api/transactions")
@Log4j2
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/buy")
    public ResponseEntity<TransactionDto> buyBtc(@RequestBody CreateTransactionRequestDto request) {
        log.info("Initiating BTC buy transaction for user ID: {}", request.getUserId());
        TransactionDto transactionDTO = transactionService.createTransaction(request, TransactionType.BUY);
        log.info("BTC buy transaction completed for user ID: {}", request.getUserId());
        return ResponseEntity.ok(transactionDTO);
    }

    @PostMapping("/sell")
    public ResponseEntity<TransactionDto> sellBtc(@RequestBody CreateTransactionRequestDto request) {
        log.info("Initiating BTC sell transaction for user ID: {}", request.getUserId());
        TransactionDto transactionDTO = transactionService.createTransaction(request, TransactionType.SELL);
        log.info("BTC sell transaction completed for user ID: {}", request.getUserId());
        return ResponseEntity.ok(transactionDTO);
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<Page<TransactionDto>> getUserTransactionHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {

        log.info("Fetching transaction history for user ID: {} with page number: {} and size: {}", userId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionDto> transactions = transactionService.getUserTransactionHistory(userId, pageable);
        log.info("Fetched {} transactions for user ID: {}", transactions.getTotalElements(), userId);
        return ResponseEntity.ok(transactions);
    }
}
