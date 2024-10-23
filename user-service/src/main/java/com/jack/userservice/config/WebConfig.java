package com.jack.userservice.config;

import com.jack.common.constants.SecurityConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@Log4j2
public class WebConfig {
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Set CORS configuration
        config.setAllowCredentials(true);

        config.addAllowedOriginPattern("*"); // Specify allowed origins
        config.addAllowedHeader("*"); // Adjust this to specific headers if possible
        config.addAllowedMethod("*"); // Adjust this to specific methods if possible
        config.addExposedHeader(SecurityConstants.AUTHORIZATION_HEADER);

        source.registerCorsConfiguration("/**", config);
        log.info("CORS filter configured with settings: {}", config);
        return new CorsFilter(source);
    }
}
