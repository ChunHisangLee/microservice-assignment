package com.jack.common.constants;

public final class TransactionConstants {

    private TransactionConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String TRANSACTION_CACHE_PREFIX = "transaction:";
    public static final long TRANSACTION_CACHE_TTL = 10;

    public static final String TRANSACTION_CREATE_QUEUE = "transaction.create.queue";
    public static final String TRANSACTION_UPDATE_QUEUE = "transaction.update.queue";
    public static final String TRANSACTION_BALANCE_QUEUE = "transaction.balance.queue";

    public static final String TRANSACTION_EXCHANGE = "transaction-exchange";

    public static final String TRANSACTION_CREATE_ROUTING_KEY = "transaction.create.routing.key";
    public static final String TRANSACTION_UPDATE_ROUTING_KEY = "transaction.update.routing.key";
    public static final String TRANSACTION_BALANCE_ROUTING_KEY = "transaction.balance.routing.key";
}
