package com.hotelreservation.controller.view;

import com.hotelreservation.dto.request.LoginRequest;
import com.hotelreservation.dto.request.RegisterRequest;
import com.hotelreservation.dto.response.AuthResponse;
import com.hotelreservation.service.UserService;
import com.hotelreservation.util.Constants;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequiredArgsConstructor
public class AuthViewController {

    private static final Logger logger = LoggerFactory.getLogger(AuthViewController.class);

    private final UserService userService;
    private final UserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequest());
        }
        return Constants.VIEW_AUTH_LOGIN;
    }

    @PostMapping("/login")
    public String processLogin(@Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
            BindingResult result, HttpServletRequest request, HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        logger.info("Processing login form submission for email: {}", loginRequest.getEmail());

        if (result.hasErrors()) {
            logger.warn("Login form validation failed: {}", result.getAllErrors());
            return Constants.VIEW_AUTH_LOGIN;
        }

        try {
            // Authenticate the user
            AuthResponse authResponse = userService.login(loginRequest);

            // Set JWT token as a cookie
            Cookie cookie = new Cookie("jwt_token", authResponse.getToken());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60); // 1 day
            response.addCookie(cookie);

            // Set authentication in SecurityContext
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Create a new SecurityContext and set the authentication
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);

            // Set the context in both the holder and the session
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            // Also store in the session directly
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            logger.info("Authentication set in SecurityContext for user: {}", loginRequest.getEmail());

            return Constants.REDIRECT_HOME;
        } catch (Exception e) {
            logger.error("Login failed for user: {}", loginRequest.getEmail(), e);
            redirectAttributes.addFlashAttribute(Constants.ATTR_ERROR_MSG, "Invalid email or password");
            redirectAttributes.addFlashAttribute("loginRequest", loginRequest);
            return Constants.REDIRECT_LOGIN;
        }
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return Constants.VIEW_AUTH_REGISTER;
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
            BindingResult result, HttpServletRequest request, HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        logger.info("Processing registration form submission for email: {}", registerRequest.getEmail());

        if (result.hasErrors()) {
            logger.warn("Registration form validation failed: {}", result.getAllErrors());
            return Constants.VIEW_AUTH_REGISTER;
        }

        try {
            // Register the user
            AuthResponse authResponse = userService.register(registerRequest);

            // Set JWT token as a cookie
            Cookie cookie = new Cookie("jwt_token", authResponse.getToken());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60); // 1 day
            response.addCookie(cookie);

            // Set authentication in SecurityContext
            UserDetails userDetails = userDetailsService.loadUserByUsername(registerRequest.getEmail());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Create a new SecurityContext and set the authentication
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);

            // Set the context in both the holder and the session
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            // Also store in the session directly
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            logger.info("Authentication set in SecurityContext for newly registered user: {}",
                    registerRequest.getEmail());

            return Constants.REDIRECT_HOME;
        } catch (Exception e) {
            logger.error("Registration failed for user: {}", registerRequest.getEmail(), e);
            redirectAttributes.addFlashAttribute(Constants.ATTR_ERROR_MSG, "Registration failed: " + e.getMessage());
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            return Constants.REDIRECT_REGISTER;
        }
    }
}
