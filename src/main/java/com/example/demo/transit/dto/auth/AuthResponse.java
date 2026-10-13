package com.example.demo.transit.dto.auth;

import com.example.demo.transit.domain.Role;

public record AuthResponse(
        String token,
        String tokenType,
        String userId,
        String fullName,
        String email,
        Role role
) {
    public static AuthResponse bearer(String token, String userId, String fullName, String email, Role role) {
        return new AuthResponse(token, "Bearer", userId, fullName, email, role);
    }
}
