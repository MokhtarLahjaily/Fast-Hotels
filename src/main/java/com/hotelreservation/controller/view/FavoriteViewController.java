package com.hotelreservation.controller.view;

import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.service.FavoriteService;
import com.hotelreservation.service.HotelService;
import com.hotelreservation.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/favorites")
public class FavoriteViewController {

    private static final Logger logger = LoggerFactory.getLogger(FavoriteViewController.class);

    private final FavoriteService favoriteService;
    private final HotelService hotelService;

    @Autowired
    public FavoriteViewController(FavoriteService favoriteService, HotelService hotelService) {
        this.favoriteService = favoriteService;
        this.hotelService = hotelService;
    }

    @GetMapping
    public String viewFavorites(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }

        try {
            String userEmail = authentication.getName();
            List<HotelResponse> favoriteHotels = favoriteService.getUserFavorites(userEmail);

            model.addAttribute("favorites", favoriteHotels);
            return "favorites/list";
        } catch (Exception e) {
            logger.error("Error viewing favorites", e);
            model.addAttribute(Constants.ATTR_ERROR_MSG, "An error occurred while loading your favorites.");
            return "favorites/list";
        }
    }

    @PostMapping("/add/{hotelId}")
    public String addFavorite(
            @PathVariable Long hotelId,
            @RequestParam(required = false) String redirectUrl,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }

        try {
            favoriteService.addFavorite(hotelId);
            redirectAttributes.addFlashAttribute(Constants.ATTR_SUCCESS_MSG, "Hotel added to favorites!");
        } catch (Exception e) {
            logger.error("Error adding favorite", e);
            redirectAttributes.addFlashAttribute(Constants.ATTR_ERROR_MSG, "Failed to add hotel to favorites.");
        }

        return isValidRedirectUrl(redirectUrl) ? "redirect:" + redirectUrl : "redirect:/hotels/" + hotelId;
    }

    @PostMapping("/remove/{hotelId}")
    public String removeFavorite(
            @PathVariable Long hotelId,
            @RequestParam(required = false) String redirectUrl,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }

        try {
            favoriteService.removeFavorite(hotelId);
            redirectAttributes.addFlashAttribute(Constants.ATTR_SUCCESS_MSG, "Hotel removed from favorites!");
        } catch (Exception e) {
            logger.error("Error removing favorite", e);
            redirectAttributes.addFlashAttribute(Constants.ATTR_ERROR_MSG, "Failed to remove hotel from favorites.");
        }

        return isValidRedirectUrl(redirectUrl) ? "redirect:" + redirectUrl : "redirect:/hotels/" + hotelId;
    }

    private boolean isValidRedirectUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        // Strict whitelist of allowed paths
        // Must start with '/' and not '//' (protocol relative)
        if (!url.startsWith("/") || url.startsWith("//") || url.contains("\\")) {
            return false;
        }

        // Optional: List of allowed base paths for even stricter security
        List<String> allowedPrefixes = List.of(
                "/hotels",
                "/favorites",
                "/profile",
                "/bookings",
                "/search",
                "/admin",
                "/owner");

        return allowedPrefixes.stream().anyMatch(url::startsWith);
    }
}
