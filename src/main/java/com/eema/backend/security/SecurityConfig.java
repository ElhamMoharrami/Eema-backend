package com.eema.backend.security;

import com.eema.backend.config.AppProperties;

import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${app.security.hsts-enabled:false}")
    private boolean hstsEnabled;

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

                .headers(headers -> {
                    headers.contentTypeOptions(contentType -> {});
                    headers.frameOptions(frame -> frame.deny());
                    headers.referrerPolicy(referrer -> referrer.policy(
                            org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
                    headers.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"));
                    if (hstsEnabled) headers.httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true).preload(true).maxAgeInSeconds(31536000));
                    else headers.httpStrictTransportSecurity(hsts -> hsts.disable());
                })

                .authorizeHttpRequests(auth -> auth

                        // -------------------------
                        // AUTH
                        // -------------------------

                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login", "/auth/refresh",
                                "/auth/verify-email", "/auth/resend-verification",
                                "/auth/forgot-password", "/auth/reset-password").permitAll()

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
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/metrics/**").hasRole("ADMIN")

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
    public PasswordEncoder passwordEncoder(
            @Value("${app.password.argon2.memory-kib:65536}") int memory,
            @Value("${app.password.argon2.iterations:3}") int iterations,
            @Value("${app.password.argon2.parallelism:1}") int parallelism) {
        // The PHC representation identifies Argon2id, version, costs and salt itself.
        if (memory < 19456 || iterations < 2 || parallelism < 1) {
            throw new IllegalArgumentException("Argon2 costs are below the supported security floor");
        }
        return new Argon2PasswordEncoder(16, 32, parallelism, memory, iterations);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(appProperties.cors().allowedOrigins());

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "X-Request-ID"));
        configuration.setExposedHeaders(List.of("X-Request-ID", "Content-Disposition"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
