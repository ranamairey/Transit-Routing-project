package com.example.demo.transit.dto;

import com.example.demo.transit.domain.Role;

public record UserResponse(String id, String fullName, String email, Role role) {
}
