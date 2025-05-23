package com.hotelreservation.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        logger.debug("Processing request: {}", requestURI);

        // Skip filter for static resources and login processing
        if (shouldSkipFilter(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if authentication is already set (e.g., from form login)
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
                !SecurityContextHolder.getContext().getAuthentication().getName().equals("anonymousUser")) {
            logger.debug("Authentication already set in SecurityContext: {}",
                    SecurityContextHolder.getContext().getAuthentication().getName());
            filterChain.doFilter(request, response);
            return;
        }

        // Try to get JWT from cookies first
        String jwt = extractJwtFromCookies(request);
        logger.debug("JWT from cookies: {}", jwt != null ? "Present" : "Not found");

        // If not found in cookies, try Authorization header
        if (jwt == null) {
            jwt = extractJwtFromHeader(request);
            logger.debug("JWT from header: {}", jwt != null ? "Present" : "Not found");
        }

        if (jwt != null) {
            try {
                // First validate the token format and expiration
                if (jwtTokenProvider.validateToken(jwt)) {
                    String username = jwtTokenProvider.getUsernameFromToken(jwt);
                    logger.debug("Username from token: {}", username);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // Now validate the token against the user details
                    if (jwtTokenProvider.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        logger.debug("Authentication set for user: {}", username);
                    } else {
                        logger.debug("Token validation failed for user: {}", username);
                    }
                } else {
                    logger.debug("Invalid token format or expired token");
                }
            } catch (Exception e) {
                logger.error("Error processing JWT token", e);
            }
        } else {
            logger.debug("No JWT token found in request");
        }

        filterChain.doFilter(request, response);

        // Log authentication state after the filter chain
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            logger.debug("Authentication after filter chain: {}",
                    SecurityContextHolder.getContext().getAuthentication().getName());
        } else {
            logger.debug("No authentication after filter chain");
        }
    }

    private boolean shouldSkipFilter(String path) {
        return path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.equals("/favicon.ico") ||
                path.equals("/login") ||
                path.equals("/register") ||
                path.startsWith("/api/auth/");
    }

    private String extractJwtFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<Cookie> jwtCookie = Arrays.stream(cookies)
                    .filter(cookie -> "jwt_token".equals(cookie.getName()))
                    .findFirst();

            if (jwtCookie.isPresent()) {
                logger.debug("JWT found in cookies");
                return jwtCookie.get().getValue();
            }
        }
        return null;
    }

    private String extractJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            logger.debug("JWT found in Authorization header");
            return bearerToken.substring(7);
        }
        logger.debug("JWT not found in Authorization header");
        return null;
    }
}
