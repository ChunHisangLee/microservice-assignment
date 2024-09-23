package com.jack.priceservice.service;

import java.math.BigDecimal;

public interface PriceService {
    void setPriceWithId(Long id, BigDecimal price);

    BigDecimal getPrice();
}
