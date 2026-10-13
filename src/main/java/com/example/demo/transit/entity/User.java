package com.example.demo.transit.entity;

import com.example.demo.transit.domain.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    private String id;

    private String fullName;

    /** Stored lower-cased. */
    @Indexed(unique = true)
    private String email;

    private String password;

    private Role role = Role.ROLE_USER;

    private boolean emailVerified = false;

    private String verificationToken;

    private String resetToken;

    private Instant resetTokenExpiresAt;
}
