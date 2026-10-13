package com.example.demo.transit.security;

import com.example.demo.transit.entity.User;
import com.example.demo.transit.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email == null ? "" : email.toLowerCase().trim())
                .orElseThrow(() -> new UsernameNotFoundException("المستخدم غير موجود: " + email));
        return new AuthUser(user.getId(), user.getEmail(), user.getPassword(), user.getRole());
    }
}
