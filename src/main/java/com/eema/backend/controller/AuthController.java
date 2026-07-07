package com.eema.backend.controller;

import com.eema.backend.dto.request.LoginRequest;
import com.eema.backend.dto.request.RefreshRequest;
import com.eema.backend.dto.request.RegisterRequest;
import com.eema.backend.dto.response.AuthResponse;
import com.eema.backend.dto.response.UserDto;
import com.eema.backend.entity.User;
import com.eema.backend.mapper.UserMapper;
import com.eema.backend.repository.UserRepository;
import com.eema.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.GetMapping;
import org.springframework.web.bind.PostMapping;
import org.springframework.web.bind.RequestBody;
import org.springframework.web.bind.RequestMapping;
import org.springframework.web.bind.RestController;

/**
 * Controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Register a new user.
     *
     * @param request registration request
     * @return authentication response with tokens
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Login a user.
     *
     * @param request login request
     * @return authentication response with tokens
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token using refresh token.
     *
     * @param request refresh token request
     * @return authentication response with new tokens
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user information.
     *
     * @return user data for the currently authenticated user
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        // Get the email from the authentication object (set by JwtAuthenticationFilter)
        String email = authentication.getName();

        // Find the user by email
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));

        // Return the user DTO
        return ResponseEntity.ok(userMapper.toDto(user));
    }
}