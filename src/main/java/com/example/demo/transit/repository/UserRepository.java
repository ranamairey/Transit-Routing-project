package com.example.demo.transit.repository;

import com.example.demo.transit.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByResetToken(String token);

    Optional<User> findByVerificationToken(String token);
}
