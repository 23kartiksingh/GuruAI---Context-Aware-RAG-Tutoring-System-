package com.guruai.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GuruAI API Gateway — Port 8080
 *
 * <p>Single entry point for all client traffic.
 * Routes requests to the appropriate microservice after JWT validation.
 *
 * <p>Package structure:
 * <pre>
 * config/       — Gateway routes, JWT filter, rate limiter config
 * filter/       — GlobalJwtAuthFilter (extends AbstractGatewayFilterFactory)
 * exception/    — GlobalExceptionHandler for gateway-level errors
 * util/         — JwtUtil (token validation, no issuance — that's Auth Service)
 * </pre>
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
