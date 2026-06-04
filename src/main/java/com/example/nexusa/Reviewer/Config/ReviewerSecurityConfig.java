package com.example.nexusa.Reviewer.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class ReviewerSecurityConfig {

    private final ReviewerJwtFilter reviewerJwtFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public ReviewerSecurityConfig(
            ReviewerJwtFilter reviewerJwtFilter,
            CorsConfigurationSource corsConfigurationSource
    ) {
        this.reviewerJwtFilter = reviewerJwtFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain reviewerFilterChain(HttpSecurity http) throws Exception {

        http
                .securityMatcher("/reviewer/**")
                .csrf(csrf -> csrf.disable())
                .cors(c -> c.configurationSource(corsConfigurationSource))
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/reviewer/auth/login",
                                "/reviewer/auth/register"
                        ).permitAll()
                        .anyRequest().hasRole("REVIEWER")
                )
                .addFilterBefore(
                        reviewerJwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}