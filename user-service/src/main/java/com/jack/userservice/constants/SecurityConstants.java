package com.jack.userservice.constants;

public class SecurityConstants {
    private SecurityConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    private static final String[] PUBLIC_URLS = {
            "/api/auth/**",
            "/public/**",
            "/h2-console/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/"
    };

    public static String[] getPublicUrls() {
        return PUBLIC_URLS.clone();
    }
}
