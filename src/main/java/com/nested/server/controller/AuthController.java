package com.nested.server.controller;

import com.nested.server.dto.AuthRequest;
import com.nested.server.dto.AuthResponse;
import com.nested.server.dto.RegisterRequest;
import com.nested.server.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    private static final String TOKEN_COOKIE_NAME = "token";
    private static final Duration TOKEN_EXPIRY = Duration.ofDays(7);

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request);
        setSecureTokenCookie(response, authResponse.getToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        setSecureTokenCookie(response, authResponse.getToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok().build();
    }

    private void setSecureTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(TOKEN_COOKIE_NAME, token)
                .httpOnly(true)           // JavaScript cannot access - XSS protection
                .secure(secureCookie)     // HTTPS only in production
                .path("/")                // Available on all paths
                .maxAge(TOKEN_EXPIRY)     // 7 days
                .sameSite("Strict")       // CSRF protection - cookie only sent for same-site requests
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
