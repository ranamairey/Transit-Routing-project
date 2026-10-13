package com.example.demo.transit.service;

import com.example.demo.transit.domain.Role;
import com.example.demo.transit.dto.auth.AuthResponse;
import com.example.demo.transit.dto.auth.LoginRequest;
import com.example.demo.transit.dto.auth.RegisterRequest;
import com.example.demo.transit.entity.User;
import com.example.demo.transit.repository.UserRepository;
import com.example.demo.transit.config.SecurityLimits;
import com.example.demo.transit.security.JwtService;
import com.example.demo.transit.security.RateLimiter;

import java.time.Duration;
import java.time.Clock;
import java.util.UUID;
import com.example.demo.transit.dto.auth.ResetPasswordRequest;
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
    private final RateLimiter rateLimiter;
    private final SecurityLimits limits;
    private final MailService mailService;
    private final Clock clock;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       RateLimiter rateLimiter,
                       SecurityLimits limits,
                       MailService mailService,
                       Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.rateLimiter = rateLimiter;
        this.limits = limits;
        this.mailService = mailService;
        this.clock = clock;
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
        user.setEmailVerified(false);
        user.setVerificationToken(newToken());
        User saved = userRepository.save(user);
        mailService.sendVerification(saved.getEmail(), saved.getVerificationToken());
        return toResponse(saved);
    }

    /** Always answers OK so the endpoint does not reveal which emails exist. */
    public void forgotPassword(String rawEmail) {
        String email = rawEmail.toLowerCase().trim();
        if (!rateLimiter.tryAcquire("forgot:" + email, 3, Duration.ofHours(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "طلبات كثيرة، حاول لاحقاً");
        }
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setResetToken(newToken());
            user.setResetTokenExpiresAt(clock.instant().plus(Duration.ofHours(1)));
            userRepository.save(user);
            mailService.sendPasswordReset(user.getEmail(), user.getResetToken());
        });
    }

    public AuthResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.token().trim())
                .filter(u -> u.getResetTokenExpiresAt() != null && u.getResetTokenExpiresAt().isAfter(clock.instant()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "رابط غير صالح أو منتهي الصلاحية"));
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        user.setEmailVerified(true); // the link proved ownership of the mailbox
        user.setVerificationToken(null);
        return toResponse(userRepository.save(user));
    }

    public AuthResponse verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "رابط غير صالح أو منتهي الصلاحية"));
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        return toResponse(userRepository.save(user));
    }

    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "المستخدم غير موجود"));
        if (user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "تم تأكيد البريد الإلكتروني مسبقاً");
        }
        if (!rateLimiter.tryAcquire("verify:" + user.getEmail(), 3, Duration.ofHours(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "طلبات كثيرة، حاول لاحقاً");
        }
        if (user.getVerificationToken() == null) {
            user.setVerificationToken(newToken());
            userRepository.save(user);
        }
        mailService.sendVerification(user.getEmail(), user.getVerificationToken());
    }

    private static String newToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();
        Duration window = Duration.ofMinutes(limits.getLoginWindowMinutes());
        if (!rateLimiter.tryAcquire("login:" + email, limits.getLoginAttempts(), window)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "محاولات دخول كثيرة، حاول مجدداً بعد " + rateLimiter.secondsUntilFree("login:" + email, window) / 60 + " دقيقة");
        }
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
        return AuthResponse.bearer(token, user.getId(), user.getFullName(), user.getEmail(), user.getRole(), user.isEmailVerified());
    }
}
