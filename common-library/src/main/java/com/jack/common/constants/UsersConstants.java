package com.jack.common.constants;

public class UsersConstants {
    private UsersConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Redis settings
    public static final String USER_CACHE_PREFIX = "users:";

}
