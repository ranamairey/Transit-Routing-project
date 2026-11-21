package com.example.demo.transit.web;

import com.example.demo.transit.dto.PushSubscribeRequest;
import com.example.demo.transit.service.PushNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Web Push for line alerts: the public VAPID key, and (signed in) subscribe / unsubscribe. */
@RestController
@RequestMapping("/api/v1/push")
public class PushController {

    private final PushNotificationService push;

    public PushController(PushNotificationService push) {
        this.push = push;
    }

    @GetMapping("/key")
    public Map<String, Object> key() {
        return push.enabled() ? Map.of("enabled", true, "publicKey", push.publicKey()) : Map.of("enabled", false);
    }

    @PostMapping("/subscribe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(Authentication auth, @Valid @RequestBody PushSubscribeRequest request) {
        push.subscribe(auth.getName(), request);
    }

    @PostMapping("/unsubscribe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@RequestBody Map<String, String> body) {
        String endpoint = body.get("endpoint");
        if (endpoint != null && !endpoint.isBlank()) {
            push.unsubscribe(endpoint);
        }
    }
}
