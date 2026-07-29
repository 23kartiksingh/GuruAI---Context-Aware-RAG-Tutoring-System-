package com.guruai.auth.service.impl;

import com.guruai.auth.config.JwtProperties;
import com.guruai.auth.dto.request.LoginRequest;
import com.guruai.auth.dto.request.RegisterRequest;
import com.guruai.auth.dto.request.UpdateProfileRequest;
import com.guruai.auth.dto.response.AuthResponse;
import com.guruai.auth.dto.response.UserProfileResponse;
import com.guruai.auth.entity.User;
import com.guruai.auth.event.producer.AuthEventProducer;
import com.guruai.auth.mapper.UserMapper;
import com.guruai.auth.repository.UserRepository;
import com.guruai.auth.service.AuthService;
import com.guruai.auth.service.TokenService;
import com.guruai.common.exception.GuruAIException;
import com.guruai.common.exception.ResourceNotFoundException;
import com.guruai.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of {@link AuthService}.
 *
 * <p>Orchestrates user registration, login, logout, and profile management.
 * Delegates token operations to {@link TokenService} and event publishing
 * to {@link AuthEventProducer}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final TokenService      tokenService;
    private final AuthEventProducer eventProducer;
    private final UserMapper        userMapper;
    private final JwtProperties     jwtProperties;

    // ── Register ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        String username = request.username().toLowerCase().trim();

        // 1. Check uniqueness
        if (userRepository.existsByUsername(username)) {
            throw new GuruAIException(HttpStatus.CONFLICT,
                    "Username '" + username + "' is already taken. Please choose a different one.");
        }

        // 2. Hash password and persist user
        String hash = passwordEncoder.encode(request.password());
        User user = new User(username, hash, request.name());
        user = userRepository.save(user);
        log.info("New user registered: username={}, userId={}", username, user.getId());

        // 3. Issue tokens and set httpOnly cookies
        String userId      = user.getId().toString();
        String accessToken = tokenService.issueTokensAndSetCookies(userId, username, response);

        // 4. Publish Kafka event (async — won't fail registration if Kafka is down)
        eventProducer.publishUserRegistered(userId, username, user.getName());

        return AuthResponse.of(accessToken, userId, username, user.getName(),
                               jwtProperties.accessExpiryMs());
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        String username = request.username().toLowerCase().trim();

        // 1. Find user (constant-time lookup — no early return on "not found" to prevent enumeration)
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        // 2. Verify BCrypt password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Failed login attempt for username={}", username);
            throw new UnauthorizedException("Invalid username or password");
        }

        // 3. Issue tokens
        String userId      = user.getId().toString();
        String accessToken = tokenService.issueTokensAndSetCookies(userId, username, response);
        log.info("User logged in: username={}, userId={}", username, userId);

        return AuthResponse.of(accessToken, userId, username, user.getName(),
                               jwtProperties.accessExpiryMs());
    }

    // ── Google OAuth2 ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(String googleId, String email, String name,
                                        HttpServletResponse response) {
        boolean[] isNewAccount = { false };

        User user = userRepository.findByAuthProviderAndProviderId("GOOGLE", googleId)
                .orElseGet(() -> {
                    isNewAccount[0] = true;
                    String username = generateUsernameFromEmail(email);
                    User created = User.forGoogleSignup(username, email, name, googleId);
                    User saved = userRepository.save(created);
                    log.info("New Google account created: username={}, userId={}",
                             username, saved.getId());
                    return saved;
                });

        String userId      = user.getId().toString();
        String accessToken = tokenService.issueTokensAndSetCookies(userId, user.getUsername(), response);

        if (isNewAccount[0]) {
            eventProducer.publishUserRegistered(userId, user.getUsername(), user.getName());
        } else {
            log.info("Google login: username={}, userId={}", user.getUsername(), userId);
        }

        return AuthResponse.of(accessToken, userId, user.getUsername(), user.getName(),
                               jwtProperties.accessExpiryMs());
    }

    /**
     * Derive a unique username from the local part of a Google email
     * (e.g. "kartik.sharma@gmail.com" → "kartiksharma"), appending a numeric
     * suffix on collision. Register never asks Google users to pick one.
     */
    private String generateUsernameFromEmail(String email) {
        String base = (email != null && email.contains("@"))
                ? email.substring(0, email.indexOf('@'))
                : "user";
        base = base.toLowerCase().replaceAll("[^a-z0-9._-]", "");
        if (base.isBlank()) {
            base = "user";
        }

        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken, HttpServletResponse response) {
        tokenService.revokeTokens(accessToken, refreshToken);
        tokenService.clearCookies(response);
        log.debug("User logged out");
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String userId) {
        User user = findUserOrThrow(userId);
        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = findUserOrThrow(userId);

        // Only update non-null fields (partial update semantics)
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.bio() != null) {
            user.setBio(request.bio().trim());
        }

        user = userRepository.save(user);
        log.debug("Profile updated for userId={}", userId);
        return userMapper.toProfileResponse(user);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User findUserOrThrow(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
