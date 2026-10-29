package com.eema.backend.security;

import com.eema.backend.config.AppProperties;

import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AppProperties appProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http

                /*
                 * REST API using JWT.
                 * No server-side session.
                 */.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                /*
                 * JWT API does not need CSRF protection.
                 */.csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeHttpRequests(auth -> auth

                        // -------------------------
                        // AUTH
                        // -------------------------

                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login", "/auth/refresh").permitAll()

                        // -------------------------
                        // MOLLIE WEBHOOK
                        // -------------------------

                        /*
                         * Must be public.
                         *
                         * Mollie cannot send your application's JWT.
                         *
                         * Actual external URL:
                         * /api/webhooks/mollie/payment
                         *
                         * Spring Security sees:
                         * /webhooks/mollie/payment
                         */.requestMatchers(HttpMethod.POST, "/webhooks/mollie/**").permitAll()

                        // -------------------------
                        // PUBLIC VENUES
                        // -------------------------

                        .requestMatchers(HttpMethod.GET, "/venues", "/venues/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/upload/files/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()

                        // Administrative operational and financial data.
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // -------------------------
                        // EVERYTHING ELSE
                        // -------------------------

                        .anyRequest().authenticated())

                /*
                 * Custom JSON responses for 401 / 403.
                 */.exceptionHandling(exceptions -> exceptions

                        .authenticationEntryPoint((request, response, authException) -> {

                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                            response.setContentType("application/json");

                            response.getWriter().write("""
                                    {
                                      "error": "Unauthorized",
                                      "message": "Authentication is required"
                                    }
                                    """);
                        })

                        .accessDeniedHandler((request, response, accessDeniedException) -> {

                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                            response.setContentType("application/json");

                            response.getWriter().write("""
                                    {
                                      "error": "Forbidden",
                                      "message": "You do not have permission to access this resource"
                                    }
                                    """);
                        }))

                .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(appProperties.cors().allowedOrigins());

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
