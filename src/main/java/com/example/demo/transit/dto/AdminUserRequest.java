package com.example.demo.transit.dto;

import com.example.demo.transit.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin creates a user (any role). */
public record AdminUserRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        Role role
) {
}
