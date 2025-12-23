package com.hotelreservation.controller.view;

import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.service.FavoriteService;
import com.hotelreservation.service.HotelService;
import com.hotelreservation.util.AppConstants;
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
                authentication.getName().equals(AppConstants.Attributes.ANONYMOUS_USER)) {
            return "redirect:" + AppConstants.Routes.LOGIN;
        }

        try {
            String userEmail = authentication.getName();
            List<HotelResponse> favoriteHotels = favoriteService.getUserFavorites(userEmail);

            model.addAttribute("favorites", favoriteHotels);
            return "favorites/list";
        } catch (Exception e) {
            logger.error("Error viewing favorites", e);
            model.addAttribute(AppConstants.Attributes.ERROR_MESSAGE,
                    "An error occurred while loading your favorites.");
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
                authentication.getName().equals(AppConstants.Attributes.ANONYMOUS_USER)) {
            return "redirect:" + AppConstants.Routes.LOGIN;
        }

        try {
            favoriteService.addFavorite(hotelId);
            redirectAttributes.addFlashAttribute(AppConstants.Attributes.SUCCESS_MESSAGE, "Hotel added to favorites!");
        } catch (Exception e) {
            logger.error("Error adding favorite", e);
            redirectAttributes.addFlashAttribute(AppConstants.Attributes.ERROR_MESSAGE,
                    "Failed to add hotel to favorites.");
        }

        return isValidRedirectUrl(redirectUrl) ? "redirect:" + redirectUrl
                : "redirect:" + AppConstants.Routes.API_HOTELS + "/" + hotelId;
    }

    @PostMapping("/remove/{hotelId}")
    public String removeFavorite(
            @PathVariable Long hotelId,
            @RequestParam(required = false) String redirectUrl,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals(AppConstants.Attributes.ANONYMOUS_USER)) {
            return "redirect:" + AppConstants.Routes.LOGIN;
        }

        try {
            favoriteService.removeFavorite(hotelId);
            redirectAttributes.addFlashAttribute(AppConstants.Attributes.SUCCESS_MESSAGE,
                    "Hotel removed from favorites!");
        } catch (Exception e) {
            logger.error("Error removing favorite", e);
            redirectAttributes.addFlashAttribute(AppConstants.Attributes.ERROR_MESSAGE,
                    "Failed to remove hotel from favorites.");
        }

        return isValidRedirectUrl(redirectUrl) ? "redirect:" + redirectUrl
                : "redirect:" + AppConstants.Routes.API_HOTELS + "/" + hotelId;
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
                AppConstants.Routes.API_HOTELS,
                "/favorites",
                "/profile",
                AppConstants.Routes.API_BOOKINGS,
                AppConstants.Routes.SEARCH,
                AppConstants.Routes.API_ADMIN,
                "/owner");

        return allowedPrefixes.stream().anyMatch(url::startsWith);
    }
}
