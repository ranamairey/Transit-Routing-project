package com.example.demo.transit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The parts of a browser PushSubscription the server needs. */
public record PushSubscribeRequest(
        @NotBlank @Size(max = 1000) String endpoint,
        @NotBlank @Size(max = 200) String p256dh,
        @NotBlank @Size(max = 100) String auth
) {
}
