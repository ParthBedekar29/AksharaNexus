    package com.example.nexusa.University.Configuration;

    import com.example.nexusa.University.Utility.JwtFilter;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.core.annotation.Order;
    import org.springframework.security.config.annotation.web.builders.HttpSecurity;
    import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
    import org.springframework.security.config.http.SessionCreationPolicy;
    import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.security.web.SecurityFilterChain;
    import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
    import org.springframework.web.cors.CorsConfiguration;
    import org.springframework.web.cors.CorsConfigurationSource;
    import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

    import java.util.List;

    @Configuration
    @EnableWebSecurity
    public class SecurityConfig {

        @Bean
        @Order(3)
        public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter filter) throws Exception{
            http.csrf(csrf->csrf.disable())
                    .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth->auth.requestMatchers("/auth/**")
                     .permitAll().anyRequest().authenticated()).cors(c->c.configurationSource(corsConfigurationSource())).addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);


            return http.build();
        }
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration config = new CorsConfiguration();

            config.setAllowedOrigins(List.of(
                    "http://localhost:63342",
                    "https://contribute-aksharanexus.netlify.app",
                    "https://reviewer-aksharanexus.netlify.app",
                    "https://aksharaoracle.netlify.app"
            ));

            config.setAllowedMethods(List.of(
                    "GET",
                    "POST",
                    "PUT",
                    "DELETE",
                    "OPTIONS",
                    "PATCH"
            ));

            config.setAllowedHeaders(List.of(
                    "Authorization",
                    "Content-Type"
            ));

            config.setAllowCredentials(false);

            UrlBasedCorsConfigurationSource source =
                    new UrlBasedCorsConfigurationSource();

            source.registerCorsConfiguration("/**", config);

            return source;
        }
        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
