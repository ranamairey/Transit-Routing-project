package com.example.demo.transit.web;

import com.example.demo.transit.domain.Role;
import com.example.demo.transit.dto.AdminUserRequest;
import com.example.demo.transit.dto.PageDto;
import com.example.demo.transit.dto.UserResponse;
import com.example.demo.transit.dto.UserUpdateRequest;
import com.example.demo.transit.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public PageDto<UserResponse> list(@RequestParam(required = false) String q,
                                      @RequestParam(required = false) Role role,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "50") int size) {
        return userAdminService.list(q, role, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody AdminUserRequest request) {
        return userAdminService.create(request);
    }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable String id, @Valid @RequestBody UserUpdateRequest request, Authentication auth) {
        return userAdminService.update(id, request, auth.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id, Authentication auth) {
        userAdminService.delete(id, auth.getName());
    }
}
