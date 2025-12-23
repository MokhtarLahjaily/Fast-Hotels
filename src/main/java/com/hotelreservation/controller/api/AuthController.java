package com.hotelreservation.controller.api;

import com.hotelreservation.dto.request.LoginRequest;
import com.hotelreservation.dto.request.RegisterRequest;
import com.hotelreservation.dto.response.AuthResponse;
import com.hotelreservation.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        logger.info("Processing registration request for email: {}", request.getEmail());
        AuthResponse authResponse = userService.register(request);

        // Set JWT token as a cookie
        setJwtCookie(response, authResponse.getToken());

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        logger.info("Processing login request for email: {}", request.getEmail());
        AuthResponse authResponse = userService.login(request);

        // Set JWT token as a cookie
        setJwtCookie(response, authResponse.getToken());

        return ResponseEntity.ok(authResponse);
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("jwt_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (24L * 60 * 60)); // 1 day - cast to long first to prevent overflow
        response.addCookie(cookie);
        logger.debug("JWT cookie set");
    }
}
