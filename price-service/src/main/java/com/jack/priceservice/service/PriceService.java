package com.jack.priceservice.service;

import java.math.BigDecimal;

public interface PriceService {
    void setPrice(BigDecimal price);

    BigDecimal getPrice();
}
