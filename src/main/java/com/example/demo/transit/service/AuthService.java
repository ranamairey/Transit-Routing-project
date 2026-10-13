package com.example.demo.transit.service;

import com.example.demo.transit.domain.Role;
import com.example.demo.transit.dto.auth.AuthResponse;
import com.example.demo.transit.dto.auth.LoginRequest;
import com.example.demo.transit.dto.auth.RegisterRequest;
import com.example.demo.transit.entity.User;
import com.example.demo.transit.repository.UserRepository;
import com.example.demo.transit.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "البريد الإلكتروني مستخدم بالفعل");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.ROLE_USER); // public registration never grants admin
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "بيانات الدخول غير صحيحة"));
        return toResponse(user);
    }

    public AuthResponse me(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "المستخدم غير موجود"));
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return AuthResponse.bearer(token, user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
