package com.hotelreservation.controller.api;

import com.hotelreservation.service.FavoriteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private static final Logger logger = LoggerFactory.getLogger(FavoriteController.class);

    private final FavoriteService favoriteService;

    @Autowired
    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/{hotelId}")
    public ResponseEntity<?> toggleFavorite(
            @PathVariable Long hotelId,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(401).body("User not authenticated");
        }

        try {
            String userEmail = authentication.getName();
            boolean isFavorite = favoriteService.isFavorite(userEmail, hotelId);

            Map<String, Object> response = new HashMap<>();

            if (isFavorite) {
                favoriteService.removeFromFavorites(userEmail, hotelId);
                response.put("status", "removed");
                response.put("message", "Hotel removed from favorites");
            } else {
                favoriteService.addToFavorites(userEmail, hotelId);
                response.put("status", "added");
                response.put("message", "Hotel added to favorites");
            }

            response.put("hotelId", hotelId);
            response.put("isFavorite", !isFavorite);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error toggling favorite for hotel {}: {}", hotelId, e.getMessage());
            return ResponseEntity.status(500).body("Error toggling favorite status");
        }
    }

    @GetMapping("/status/{hotelId}")
    public ResponseEntity<?> checkFavoriteStatus(
            @PathVariable Long hotelId,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.ok(Map.of("isFavorite", false));
        }

        try {
            String userEmail = authentication.getName();
            boolean isFavorite = favoriteService.isFavorite(userEmail, hotelId);

            return ResponseEntity.ok(Map.of("isFavorite", isFavorite));
        } catch (Exception e) {
            logger.error("Error checking favorite status for hotel {}: {}", hotelId, e.getMessage());
            return ResponseEntity.ok(Map.of("isFavorite", false));
        }
    }
}
