package com.example.demo.transit.web;

import com.example.demo.transit.domain.SuggestionStatus;
import com.example.demo.transit.dto.SuggestionAcceptRequest;
import com.example.demo.transit.dto.SuggestionResponse;
import com.example.demo.transit.service.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/suggestions")
public class AdminSuggestionController {

    private final SuggestionService service;

    public AdminSuggestionController(SuggestionService service) {
        this.service = service;
    }

    @GetMapping
    public List<SuggestionResponse> list(@RequestParam(required = false) SuggestionStatus status) {
        return service.list(status);
    }

    @PostMapping("/{id}/accept")
    public SuggestionResponse accept(@PathVariable String id, Authentication auth,
                                     @Valid @RequestBody(required = false) SuggestionAcceptRequest edits) {
        return service.accept(id, auth.getName(), edits);
    }

    @PostMapping("/{id}/reject")
    public SuggestionResponse reject(@PathVariable String id, Authentication auth) {
        return service.reject(id, auth.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
