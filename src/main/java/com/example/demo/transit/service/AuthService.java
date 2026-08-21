package com.example.demo.transit.service;

import com.example.demo.transit.domain.Role;
import com.example.demo.transit.dto.auth.AuthResponse;
import com.example.demo.transit.dto.auth.LoginRequest;
import com.example.demo.transit.dto.auth.RegisterRequest;
import com.example.demo.transit.entity.User;
import com.example.demo.transit.repository.UserRepository;
import com.example.demo.transit.security.JwtService; // افتراض وجود كلاس لإنشاء JWT
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "البريد الإلكتروني مستخدم بالفعل");
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.ROLE_USER); // إسناد صريح ومغلق لكل من يسجل من الخارج
        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        return AuthResponse.bearer(token, savedUser.getId(), savedUser.getEmail(), savedUser.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "بيانات الدخول غير صحيحة"));

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());

        return AuthResponse.bearer(token, user.getId(), user.getEmail(), user.getRole());
    }
}