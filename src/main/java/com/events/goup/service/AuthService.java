package com.events.goup.service;

import com.events.goup.dto.auth.AuthResponse;
import com.events.goup.dto.auth.LoginRequest;
import com.events.goup.dto.auth.RegisterRequest;
import com.events.goup.dto.user.UserResponse;
import com.events.goup.entity.User;
import com.events.goup.entity.enums.Role;
import com.events.goup.exception.DuplicateNameException;
import com.events.goup.exception.InvalidCredentialsException;
import com.events.goup.repository.UserRepository;
import com.events.goup.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String name = requireField(request.name(), "name");
        String rawPassword = requireField(request.password(), "password");

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateNameException("Já existe uma conta com o e-mail: " + email);
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(Role.USER);

        User saved = userRepository.save(user);
        return buildAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("E-mail ou senha inválidos"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("E-mail ou senha inválidos");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        UserResponse userResponse = toResponse(user);

        return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds(), userResponse);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), user.getCreatedAt());
    }

    private String normalizeEmail(String email) {
        return requireField(email, "email").toLowerCase().trim();
    }

    private String requireField(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("O campo " + fieldName + " é obrigatório");
        }
        return value.trim();
    }
}
