package com.example.demo.transit.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(@NotBlank String token) {
}
