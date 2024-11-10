package com.jack.common.constants;

public final class ApplicationConstants {

    // Pagination defaults
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final String BTC_PRICE_KEY = "BTC_CURRENT_PRICE";

    private ApplicationConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
