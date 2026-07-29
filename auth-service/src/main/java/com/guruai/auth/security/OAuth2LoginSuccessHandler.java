package com.guruai.auth.security;

import com.guruai.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Runs once Spring Security has finished the Google authorization-code
 * exchange and fetched the user's profile. Bridges the OAuth2 world back
 * into this service's own JWT world: find-or-create the {@code User} row,
 * issue the exact same access/refresh cookies password login would, then
 * send the browser back to the SPA.
 *
 * <p>No new frontend "callback page" is needed for this — the SPA's
 * {@code AuthContext} already silently restores a session from the
 * refresh_token cookie on mount (see {@code lib/auth.ts#refresh}), so
 * landing back on any route with the cookies already set is enough.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${guruai.oauth2.success-redirect-url}")
    private String successRedirectUrl;

    @Value("${guruai.oauth2.failure-redirect-url}")
    private String failureRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String googleId = oauth2User.getAttribute("sub");
        String email    = oauth2User.getAttribute("email");
        String name     = oauth2User.getAttribute("name");

        if (googleId == null || googleId.isBlank()) {
            // Shouldn't happen — Google always sends "sub" — but don't 500 on it.
            log.warn("Google OAuth2 login succeeded with no 'sub' claim in the profile");
            response.sendRedirect(failureRedirectUrl);
            return;
        }

        authService.loginWithGoogle(googleId, email, name, response);

        // Drop the short-lived HttpSession Spring used to correlate the
        // authorization-request state across the Google redirect round-trip.
        // Everything from here on is stateless JWT, same as password login —
        // see SecurityConfig's IF_REQUIRED session policy comment.
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        response.sendRedirect(successRedirectUrl);
    }
}
