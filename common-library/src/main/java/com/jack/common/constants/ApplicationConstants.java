package com.jack.common.constants;

import java.math.BigDecimal;

public class ApplicationConstants {

    private ApplicationConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Pagination defaults
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;

    public static final BigDecimal INITIAL_PRICE = BigDecimal.valueOf(100);
    public static final String BTC_PRICE_KEY = "BTC_CURRENT_PRICE";
}
