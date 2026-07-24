package com.guruai.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * GuruAI Auth Service — Port 8081  |  DB: auth_db
 *
 * <p>Handles all authentication and user identity concerns.
 *
 * <p>Package structure:
 * <pre>
 * config/
 *   ├── SecurityConfig.java       — Spring Security 6, BCrypt, stateless JWT
 *   ├── KafkaProducerConfig.java  — Kafka producer factory
 *   └── RedisConfig.java          — Redis for JWT blacklist / refresh token store
 *
 * controller/
 *   ├── AuthController.java       — /auth/register, /auth/login, /auth/logout, /auth/refresh
 *   └── UserController.java       — /auth/user/profile (GET/PUT), /auth/user/stats
 *
 * service/
 *   ├── AuthService.java          — interface
 *   ├── TokenService.java         — interface
 *   └── impl/
 *       ├── AuthServiceImpl.java  — register/login/logout logic
 *       └── TokenServiceImpl.java — JWT create, validate, blacklist, refresh
 *
 * repository/
 *   ├── UserRepository.java
 *   └── RefreshTokenRepository.java
 *
 * entity/
 *   ├── User.java                 — users table
 *   └── RefreshToken.java         — refresh_tokens table
 *
 * dto/
 *   ├── request/
 *   │   ├── RegisterRequest.java
 *   │   ├── LoginRequest.java
 *   │   └── UpdateProfileRequest.java
 *   └── response/
 *       ├── AuthResponse.java     — {accessToken, userId, username}
 *       ├── UserProfileResponse.java
 *       └── UserStatsResponse.java
 *
 * event/
 *   └── producer/
 *       └── AuthEventProducer.java — publishes UserRegisteredEvent
 *
 * exception/
 *   └── GlobalExceptionHandler.java
 *
 * mapper/
 *   └── UserMapper.java
 *
 * util/
 *   └── JwtUtil.java
 * </pre>
 */
@SpringBootApplication
@EnableKafka
@EnableAsync
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
