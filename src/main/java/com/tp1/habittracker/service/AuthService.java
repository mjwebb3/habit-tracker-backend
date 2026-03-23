package com.tp1.habittracker.service;

import com.tp1.habittracker.domain.model.User;
import com.tp1.habittracker.dto.user.LoginRequest;
import com.tp1.habittracker.dto.user.RegisterRequest;
import com.tp1.habittracker.exception.DuplicateResourceException;
import com.tp1.habittracker.repository.UserRepository;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public User register(RegisterRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        String normalizedUsername = request.username().trim();
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        String rawPassword = request.password().trim();

        if (rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = User.builder()
                .username(normalizedUsername)
                .email(normalizedEmail)
                .password(passwordEncoder.encode(rawPassword))
                .build();

        return userRepository.save(user);
    }

    public String login(LoginRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        String rawPassword = request.password();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (user.getPassword() == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return jwtService.generateToken(user);
    }
}