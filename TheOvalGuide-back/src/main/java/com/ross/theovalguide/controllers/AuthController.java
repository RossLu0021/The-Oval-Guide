package com.ross.theovalguide.controllers;

import com.ross.theovalguide.DTOS.auth.BasicResponse;
import com.ross.theovalguide.DTOS.auth.ForgotPasswordRequest;
import com.ross.theovalguide.DTOS.auth.LoginRequest;
import com.ross.theovalguide.DTOS.auth.RegisterRequest;
import com.ross.theovalguide.DTOS.auth.UpdatePasswordRequest;
import com.ross.theovalguide.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    @Value("${app.auth.cookie.name:oval_session}")
    private String authCookieName;

    @Value("${app.auth.cookie.secure:false}")
    private boolean cookieSecure;

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<BasicResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = authService.login(request.login(), request.password());

        ResponseCookie cookie = buildAuthCookie(result.token(), result.ttl());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(BasicResponse.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<BasicResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.username(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(BasicResponse.OK);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<BasicResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.initiatePasswordReset(request.email());
        return ResponseEntity.ok(BasicResponse.OK);
    }

    @PostMapping("/update-password")
    public ResponseEntity<BasicResponse> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        authService.updatePassword(request.token(), request.password());
        return ResponseEntity.ok(BasicResponse.OK);
    }

    @ExceptionHandler(AuthService.AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(AuthService.AuthException exception) {
        String message = exception.getMessage() == null ? "Request failed" : exception.getMessage();
        return ResponseEntity.status(exception.getStatus())
                .body(Map.of("message", message));
    }

    private ResponseCookie buildAuthCookie(String token, Duration ttl) {
        return ResponseCookie.from(authCookieName, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(ttl)
                .build();
    }
}
