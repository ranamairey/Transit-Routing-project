package com.example.demo.transit.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long jwtExpiration;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long jwtExpiration) {
        this.signingKey = deriveKey(secret);
        this.jwtExpiration = jwtExpiration;
    }

    public String generateToken(String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(signingKey)
                .compact();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Accepts either a base64 secret or any plain string; the secret is hashed with SHA-256 so the
     * HMAC key is always 256 bits long (jjwt rejects shorter keys).
     */
    private static SecretKey deriveKey(String secret) {
        byte[] raw;
        try {
            raw = Decoders.BASE64.decode(secret);
        } catch (RuntimeException notBase64) {
            raw = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (raw.length < 32) {
            try {
                raw = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
        return Keys.hmacShaKeyFor(raw);
    }
}
