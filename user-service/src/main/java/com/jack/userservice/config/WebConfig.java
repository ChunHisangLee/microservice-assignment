package com.jack.userservice.config;

import com.jack.userservice.constants.SecurityConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class WebConfig {
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");  // Allow all origins (change if needed)
        config.addAllowedHeader("*");  // Allow all headers (including Authorization)
        config.addAllowedMethod("*");  // Allow all HTTP methods (GET, POST, etc.)
        config.addExposedHeader(SecurityConstants.AUTHORIZATION_HEADER);  // Expose Authorization header to clients
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
