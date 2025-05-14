package com.hotelreservation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthDebugController {

    private static final Logger logger = LoggerFactory.getLogger(AuthDebugController.class);

    @GetMapping("/auth-debug")
    public String debugAuth(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        model.addAttribute("authName", auth != null ? auth.getName() : "null");
        model.addAttribute("isAuthenticated", auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser"));
        model.addAttribute("authorities", auth != null ? auth.getAuthorities() : "null");
        model.addAttribute("principal", auth != null ? auth.getPrincipal() : "null");

        logger.info("Auth debug - Name: {}, Authenticated: {}, Authorities: {}",
                auth != null ? auth.getName() : "null",
                auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser"),
                auth != null ? auth.getAuthorities() : "null");

        return "auth/debug";
    }
}