package com.example.demo.transit.web;

import com.example.demo.transit.dto.auth.AuthResponse;
import com.example.demo.transit.dto.auth.LoginRequest;
import com.example.demo.transit.dto.auth.RegisterRequest;
import com.example.demo.transit.dto.auth.ForgotPasswordRequest;
import com.example.demo.transit.dto.auth.ResetPasswordRequest;
import com.example.demo.transit.dto.auth.TokenRequest;
import java.util.Map;
import com.example.demo.transit.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** Sends a reset link when the email exists; always 200 so emails cannot be enumerated. */
    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return Map.of("status", "ok");
    }

    @PostMapping("/reset-password")
    public AuthResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @PostMapping("/verify-email")
    public AuthResponse verifyEmail(@Valid @RequestBody TokenRequest request) {
        return authService.verifyEmail(request.token());
    }

    @PostMapping("/resend-verification")
    public Map<String, String> resendVerification(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "هذه العملية تتطلب تسجيل الدخول");
        }
        authService.resendVerification(authentication.getName());
        return Map.of("status", "ok");
    }

    /** Refreshes the token and returns the current profile. */
    @GetMapping("/me")
    public AuthResponse me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "هذه العملية تتطلب تسجيل الدخول");
        }
        return authService.me(authentication.getName());
    }
}
