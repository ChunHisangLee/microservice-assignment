package com.jack.priceservice.controller;

import com.jack.priceservice.service.PriceService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/price")
@Log4j2
public class PriceController {
    private final PriceService priceService;

    @Autowired
    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping("/current")
    public ResponseEntity<BigDecimal> getCurrentPrice() {
        log.info("Received request to get the current BTC price.");
        BigDecimal currentPrice = priceService.getPrice();
        log.info("Current BTC price retrieved: {}", currentPrice);
        return ResponseEntity.ok(currentPrice);
    }
}
