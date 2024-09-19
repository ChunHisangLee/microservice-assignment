package com.jack.authservice.security;

import com.jack.common.constants.SecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${security.authentication.enabled:true}")
    private boolean authenticationEnabled;

    public WebSecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring security filter chain");

        if (authenticationEnabled) {
            logger.info("Authentication is ENABLED");
            http
                    .csrf(AbstractHttpConfigurer::disable)  // Disable CSRF as we're using JWT tokens
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers(SecurityConstants.getPublicUrls()).permitAll()  // Allow public URLs
                            .anyRequest().authenticated()  // Secure all other endpoints
                    )
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // Stateless session
                    .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);  // JWT filter before UsernamePasswordAuthenticationFilter
        } else {
            logger.info("Authentication is DISABLED");
            http
                    .csrf(AbstractHttpConfigurer::disable)  // Disable CSRF
                    .authorizeHttpRequests(authorize -> authorize
                            .anyRequest().permitAll()  // Allow all requests when authentication is disabled
                    );
        }

        logger.info("Security filter chain successfully configured");
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        logger.info("Creating AuthenticationManager");
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        logger.info("Creating PasswordEncoder (BCrypt)");
        return new BCryptPasswordEncoder();
    }
}
