package com.eema.backend.controller;

import com.eema.backend.dto.request.LoginRequest;
import com.eema.backend.dto.request.RefreshRequest;
import com.eema.backend.dto.request.RegisterRequest;
import com.eema.backend.dto.response.AuthResponse;
import com.eema.backend.dto.response.UserDto;
import com.eema.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for authentication endpoints.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user.
     *
     * @param request registration request
     * @return authentication response with tokens
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
     * @param authentication the current authentication
     * @return user data for the currently authenticated user
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        UserDto userDto = authService.getCurrentUser(authentication);
        return ResponseEntity.ok(userDto);
    }
}