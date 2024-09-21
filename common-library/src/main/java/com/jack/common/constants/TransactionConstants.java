package com.jack.common.constants;

public class TransactionConstants {

    private TransactionConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Transaction or event-related constants
    public static final String TRANSACTION_CREATE_QUEUE = "transaction.create.queue";
    public static final String TRANSACTION_UPDATE_QUEUE = "transaction.update.queue";
    public static final String TRANSACTION_BALANCE_QUEUE = "transaction.balance.queue";

    public static final String TRANSACTION_EXCHANGE = "transaction-exchange";

    public static final String TRANSACTION_CREATE_ROUTING_KEY = "transaction.creation";
    public static final String TRANSACTION_UPDATE_ROUTING_KEY = "transaction.update";
    public static final String TRANSACTION_BALANCE_ROUTING_KEY = "transaction.balance";
}
