package com.example.demo.transit.web;

import com.example.demo.transit.dto.AlertDto;
import com.example.demo.transit.entity.SavedPlace;
import com.example.demo.transit.service.AccountFeaturesService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/** The signed-in rider's saved places, followed lines and alerts on those lines. */
@RestController
@RequestMapping("/api/v1/me")
public class AccountController {

    private final AccountFeaturesService service;

    public AccountController(AccountFeaturesService service) {
        this.service = service;
    }

    @GetMapping("/places")
    public List<SavedPlace> places(Authentication auth) {
        return service.places(auth.getName());
    }

    @PutMapping("/places")
    public List<SavedPlace> savePlaces(Authentication auth, @RequestBody List<SavedPlace> places) {
        return service.savePlaces(auth.getName(), places);
    }

    @GetMapping("/follows")
    public Set<String> follows(Authentication auth) {
        return service.follows(auth.getName());
    }

    @PutMapping("/follows")
    public Set<String> saveFollows(Authentication auth, @RequestBody List<String> routeIds) {
        return service.saveFollows(auth.getName(), routeIds);
    }

    @GetMapping("/alerts")
    public List<AlertDto> alerts(Authentication auth) {
        return service.alerts(auth.getName());
    }
}
