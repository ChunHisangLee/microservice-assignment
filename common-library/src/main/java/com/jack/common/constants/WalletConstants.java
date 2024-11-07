package com.jack.common.constants;

public final class WalletConstants {

    private WalletConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Redis settings
    public static final String WALLET_CACHE_PREFIX = "wallet:";

    // Reply-to queue
    public static final String WALLET_REPLY_TO_QUEUE = "user-service.response.queue";

    // Wallet message queues
    public static final String WALLET_CREATE_QUEUE = "wallet.create.queue";
    public static final String WALLET_UPDATE_QUEUE = "wallet.update.queue";
    public static final String WALLET_BALANCE_QUEUE = "wallet.balance.queue";

    // Wallet message exchange
    public static final String WALLET_EXCHANGE = "wallet-exchange";

    // Routing keys for wallet operations
    public static final String WALLET_CREATE_ROUTING_KEY = "wallet.create.routing.key";
    public static final String WALLET_UPDATE_ROUTING_KEY = "wallet.update.routing.key";
    public static final String WALLET_BALANCE_ROUTING_KEY = "wallet.balance.routing.key";

    public static final String WALLET_CREATE = "WALLET_CREATE";
    public static final String WALLET_UPDATE = "WALLET_UPDATE";
}
