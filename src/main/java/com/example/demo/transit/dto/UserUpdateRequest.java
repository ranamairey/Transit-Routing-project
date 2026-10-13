package com.example.demo.transit.dto;

import com.example.demo.transit.domain.Role;
import jakarta.validation.constraints.Size;

/** Admin edits a user: any field may be null to keep the current value. */
public record UserUpdateRequest(
        String fullName,
        Role role,
        @Size(min = 6, max = 72) String newPassword
) {
}
