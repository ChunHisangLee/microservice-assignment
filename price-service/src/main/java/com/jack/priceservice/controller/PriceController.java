package com.jack.priceservice.controller;

import com.jack.priceservice.service.PriceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/price")
public class PriceController {

    private static final Logger logger = LoggerFactory.getLogger(PriceController.class);

    private final PriceService priceService;

    @Autowired
    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping("/current")
    public ResponseEntity<BigDecimal> getCurrentPrice() {
        logger.info("Received request to get the current BTC price.");

        BigDecimal currentPrice = priceService.getPrice();

        logger.info("Current BTC price retrieved: {}", currentPrice);

        return ResponseEntity.ok(currentPrice);
    }
}
