package com.example.demo.transit.dto.auth;

import com.example.demo.transit.domain.Role;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String email,
        Role role
) {
    public static AuthResponse bearer(String token, Long userId, String email, Role role) {
        return new AuthResponse(token, "Bearer", userId, email, role);
    }
}
