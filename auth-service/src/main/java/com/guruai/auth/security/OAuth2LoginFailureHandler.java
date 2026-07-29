package com.guruai.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Runs when the Google authorization-code exchange fails (user denied
 * consent, state mismatch, Google-side error, etc.). Sends the browser
 * back to the login page with a generic error flag rather than showing
 * Spring's default whitelabel error page.
 */
@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Value("${guruai.oauth2.failure-redirect-url}")
    private String failureRedirectUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("Google OAuth2 login failed: {}", exception.getMessage());
        response.sendRedirect(failureRedirectUrl);
    }
}
