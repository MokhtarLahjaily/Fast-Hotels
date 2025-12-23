package com.hotelreservation.security;

import com.hotelreservation.util.AppConstants;

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

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger jwtLogger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String servletPath = request.getServletPath();
        jwtLogger.debug("Processing request: {}", servletPath);

        // Skip filter for static resources and public routes
        if (shouldSkipFilter(servletPath)) {
            jwtLogger.debug("Skipping filter for path: {}", servletPath);
            filterChain.doFilter(request, response);
            return;
        }

        // Check if authentication is already set (e.g., from form login or session)
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
                !SecurityContextHolder.getContext().getAuthentication().getName()
                        .equals(AppConstants.Attributes.ANONYMOUS_USER)) {
            jwtLogger.debug("Authentication already set in SecurityContext: {}",
                    SecurityContextHolder.getContext().getAuthentication().getName());
            filterChain.doFilter(request, response);
            return;
        }

        // Try to get JWT from cookies first
        String jwt = extractJwtFromCookies(request);

        // If not found in cookies, try Authorization header
        if (jwt == null) {
            jwt = extractJwtFromHeader(request);
        }

        if (jwt != null) {
            try {
                if (jwtTokenProvider.validateToken(jwt)) {
                    String username = jwtTokenProvider.getUsernameFromToken(jwt);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (jwtTokenProvider.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        jwtLogger.debug("Authentication set for user: {}", username);
                    }
                }
            } catch (Exception e) {
                jwtLogger.error("Error processing JWT token for path: " + servletPath, e);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkipFilter(String path) {
        return path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.startsWith("/fonts/") ||
                path.startsWith("/webjars/") ||
                path.startsWith("/static/") ||
                path.equals("/favicon.ico") ||
                path.equals("/login") ||
                path.equals("/register") ||
                path.startsWith("/api/auth/") ||
                path.equals("/error") ||
                path.equals("/about") ||
                path.equals("/contact") ||
                path.equals("/") ||
                path.equals("/search");
    }

    private String extractJwtFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> AppConstants.Attributes.JWT_COOKIE_NAME.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private String extractJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
