package com.jack.common.constants;

public final class UsersConstants {
    private UsersConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Redis settings
    public static final String USER_CACHE_PREFIX = "users:";

    // User message queues
    public static final String USER_CREATE_QUEUE = "user.create.queue";
    public static final String USER_UPDATE_QUEUE = "user.update.queue";

    // User message exchange
    public static final String USER_EXCHANGE = "user-exchange";

    // Routing keys for user operations
    public static final String USER_CREATE_ROUTING_KEY = "user.creation";
    public static final String USER_UPDATE_ROUTING_KEY = "user.update";
}
