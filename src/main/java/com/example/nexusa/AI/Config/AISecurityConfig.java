package com.example.nexusa.AI.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class AISecurityConfig {

    private final AIJwtFilter aiJwtFilter;

    public AISecurityConfig(AIJwtFilter aiJwtFilter) {
        this.aiJwtFilter = aiJwtFilter;
    }

    @Bean
    public SecurityFilterChain aiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/oracle/**", "/auth/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(aiCorsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/oracle/**").hasRole("VIEWER")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(aiJwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource aiCorsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "https://aksharaoracle.netlify.app"
        ));

        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/oracle/**", config);
        source.registerCorsConfiguration("/auth/**", config);

        return source;
    }
}