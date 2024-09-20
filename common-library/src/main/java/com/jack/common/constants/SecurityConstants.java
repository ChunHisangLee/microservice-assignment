package com.jack.common.constants;

public class SecurityConstants {

    private SecurityConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Constants for headers and tokens
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;

    // Enum for public URLs
    public enum PublicUrls {
        API_AUTH("/api/auth/**"),
        PUBLIC("/public/**"),
        H2_CONSOLE("/h2-console/**"),
        SWAGGER_UI("/swagger-ui.html"),
        SWAGGER_RESOURCES("/swagger-resources/**"),
        API_DOCS("/v3/api-docs/**"),
        SWAGGER_UI_PATH("/swagger-ui/**"),
        ROOT("/");

        private final String url;

        PublicUrls(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

    public static String[] getPublicUrls() {
        return new String[]{
                PublicUrls.API_AUTH.getUrl(),
                PublicUrls.PUBLIC.getUrl(),
                PublicUrls.H2_CONSOLE.getUrl(),
                PublicUrls.SWAGGER_UI.getUrl(),
                PublicUrls.SWAGGER_RESOURCES.getUrl(),
                PublicUrls.API_DOCS.getUrl(),
                PublicUrls.SWAGGER_UI_PATH.getUrl(),
                PublicUrls.ROOT.getUrl()
        };
    }
}
