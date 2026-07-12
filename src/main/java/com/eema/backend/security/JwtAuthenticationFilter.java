package com.eema.backend.security;

import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.mapper.UserMapper;
import com.eema.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter for validating JWT tokens and setting security context.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7); // Remove "Bearer " prefix

        try {
            // Validate token and extract claims
            var claims = jwtService.validateToken(token);
            UUID userId = jwtService.extractUserId(token);
            String email = jwtService.extractEmail(token);
            UserRole role = jwtService.extractRole(token);

            // Load user from database (optional - we trust the JWT claims)
            // But we still check if user exists and is active
            var userOptional = userRepository.findById(userId);

            if (userOptional.isPresent() && userOptional.get().isActive()) {
                User user = userOptional.get();
                var userDetails = org.springframework.security.core.userdetails.User
                        .withUsername(email)
                        .password("") // password not needed as we trust the token
                        .authorities("ROLE_" + role.name())
                        .build();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            // If user not found or inactive, we leave SecurityContext empty (unauthenticated)

        } catch (Exception e) {
            // Log the exception but don't throw - just continue unauthenticated
            log.debug("Failed to process JWT token: {}", e.getMessage());
            // SecurityContext remains empty
        }

        filterChain.doFilter(request, response);
    }
}