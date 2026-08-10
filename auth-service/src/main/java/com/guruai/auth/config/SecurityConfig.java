package com.guruai.auth.config;

import com.guruai.auth.security.OAuth2LoginFailureHandler;
import com.guruai.auth.security.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;

/**
 * Spring Security 6 configuration — stateless JWT authentication, plus
 * "Sign in with Google" via the standard OAuth2 authorization-code flow.
 *
 * <p>Strategy: every request is authenticated by {@link JwtAuthFilter}.
 * No CSRF (stateless API). Sessions are {@code IF_REQUIRED} rather than
 * fully stateless — Spring's default OAuth2 authorization-request
 * repository needs a short-lived {@code HttpSession} to correlate the
 * Google redirect round-trip (state/PKCE). Nothing else in this service
 * ever touches the session, and {@link OAuth2LoginSuccessHandler} drops
 * it immediately once tokens are issued — so in practice every request
 * outside the Google handshake stays exactly as stateless as before.
 *
 * <p>Public endpoints: /auth/register, /auth/login, /auth/refresh,
 * /auth/oauth2/**, /actuator/health. All others require a valid JWT.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, CookieProperties.class})
public class SecurityConfig {

    // ── Public paths — no JWT required ───────────────────────────────────────
    private static final String[] PUBLIC_PATHS = {
            "/auth/register",
            "/auth/login",
            "/auth/refresh",
            "/auth/oauth2/**",
            "/actuator/health",
            "/actuator/info"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthFilter jwtAuthFilter,
                                           OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                                           OAuth2LoginFailureHandler oAuth2LoginFailureHandler) throws Exception {
        return http
                // ── Disable CSRF — stateless API uses JWT, no cookies for state ──
                .csrf(AbstractHttpConfigurer::disable)

                // ── CORS is handled once, at the edge, by api-gateway's globalcors
                // config (see application.yml there) — every request that reaches
                // this service already came through the gateway. A second CORS
                // filter here used to duplicate the Access-Control-Allow-Origin
                // header on the way back through the gateway, which browsers
                // reject outright ("contains multiple values ... only one is
                // allowed"), breaking register/login/refresh entirely. Leave CORS
                // ownership to the gateway alone.
                .cors(AbstractHttpConfigurer::disable)

                // ── See class doc — IF_REQUIRED only for the Google handshake ────
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // ── Route security rules ─────────────────────────────────────────
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated()
                )

                // ── Google OAuth2 login — both endpoints kept under /auth/** so
                // they ride the existing api-gateway route/public-path config
                // instead of needing new ones for Spring's default /oauth2/**
                // and /login/oauth2/** paths.
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(a -> a.baseUri("/auth/oauth2/authorization"))
                        .redirectionEndpoint(r -> r.baseUri("/auth/oauth2/callback/*"))
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler)
                )

                // ── JWT filter runs before the standard auth filter ──────────────
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    /**
     * BCrypt password encoder with default strength (10 rounds).
     * Shared bean — injected into AuthServiceImpl for hashing and verification.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Built by hand instead of Spring Boot's {@code spring.security.oauth2.client.*}
     * property auto-configuration, which would fail startup with a blank
     * client-id/secret. Here, missing credentials just leave the "google"
     * registration unresolvable — {@code /auth/oauth2/authorization/google}
     * 404s instead of the whole service refusing to boot. See .env.example
     * for how to create real Google Cloud OAuth credentials.
     *
     * <p>Google's endpoints are hard-coded rather than resolved via OIDC
     * issuer discovery ({@code ClientRegistrations.fromOidcIssuerLocation})
     * on purpose — discovery makes a live HTTPS call to accounts.google.com
     * at bean-creation time, so a flaky network at container boot would take
     * the whole service down with it. These URLs are Google's stable,
     * long-published OAuth2/OIDC endpoints and don't change.
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${GOOGLE_OAUTH_CLIENT_ID:}") String clientId,
            @Value("${GOOGLE_OAUTH_CLIENT_SECRET:}") String clientSecret,
            @Value("${OAUTH2_REDIRECT_BASE_URL:http://localhost:8080}") String redirectBaseUrl) {

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            return registrationId -> null;
        }

        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                // Explicit rather than a "{baseUrl}" template — this request
                // goes through api-gateway, and computing "baseUrl" correctly
                // behind a reverse proxy needs forwarded-headers wiring this
                // service doesn't otherwise need.
                .redirectUri(redirectBaseUrl + "/auth/oauth2/callback/{registrationId}")
                .scope("openid", "email", "profile")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .clientName("Google")
                .build();

        return new InMemoryClientRegistrationRepository(google);
    }
}
