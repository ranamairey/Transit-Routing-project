package com.example.demo.transit.web;

import com.example.demo.transit.dto.SuggestionRequest;
import com.example.demo.transit.dto.SuggestionResponse;
import com.example.demo.transit.service.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Riders suggest a missing place, a stop in the wrong place or a wrong line; admins review them. */
@RestController
@RequestMapping("/api/v1/suggestions")
public class SuggestionController {

    private final SuggestionService service;

    public SuggestionController(SuggestionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuggestionResponse submit(Authentication auth, @Valid @RequestBody SuggestionRequest request) {
        return service.submit(auth.getName(), request);
    }
}
