package com.example.demo.transit.service;

import com.example.demo.transit.domain.Role;
import com.example.demo.transit.dto.AdminUserRequest;
import com.example.demo.transit.dto.PageDto;
import com.example.demo.transit.dto.UserResponse;
import com.example.demo.transit.dto.UserUpdateRequest;
import com.example.demo.transit.entity.User;
import com.example.demo.transit.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Admin user management: list/search, create (any role), change role or name, reset password, delete. */
@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PageDto<UserResponse> list(String query, Role role, int page, int size) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<User> all = userRepository.findAll().stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> q.isEmpty()
                        || u.getEmail().contains(q)
                        || (u.getFullName() != null && u.getFullName().toLowerCase(Locale.ROOT).contains(q)))
                .sorted(Comparator.comparing(User::getEmail))
                .toList();
        int safeSize = Math.max(1, Math.min(size, 200));
        int safePage = Math.max(0, page);
        List<UserResponse> items = all.stream().skip((long) safePage * safeSize).limit(safeSize).map(this::toResponse).toList();
        return new PageDto<>(items, safePage, safeSize, all.size());
    }

    public UserResponse create(AdminUserRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "البريد الإلكتروني مستخدم بالفعل");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role() != null ? request.role() : Role.ROLE_USER);
        return toResponse(userRepository.save(user));
    }

    public UserResponse update(String id, UserUpdateRequest request, String actingEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "المستخدم غير موجود"));
        if (request.role() != null && request.role() != user.getRole()) {
            if (user.getEmail().equalsIgnoreCase(actingEmail)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "لا يمكنك تغيير صلاحياتك الخاصة");
            }
            if (user.getRole() == Role.ROLE_ADMIN && countAdmins() <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "يجب أن يبقى مشرف واحد على الأقل");
            }
            user.setRole(request.role());
        }
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.newPassword()));
        }
        return toResponse(userRepository.save(user));
    }

    public void delete(String id, String actingEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "المستخدم غير موجود"));
        if (user.getEmail().equalsIgnoreCase(actingEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "لا يمكنك حذف حسابك الخاص");
        }
        if (user.getRole() == Role.ROLE_ADMIN && countAdmins() <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "يجب أن يبقى مشرف واحد على الأقل");
        }
        userRepository.delete(user);
    }

    private long countAdmins() {
        return userRepository.findAll().stream().filter(u -> u.getRole() == Role.ROLE_ADMIN).count();
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole());
    }
}
