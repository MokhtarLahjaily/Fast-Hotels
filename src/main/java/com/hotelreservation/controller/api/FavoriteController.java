package com.hotelreservation.controller.api;

import com.hotelreservation.dto.response.FavoriteResponse;
import com.hotelreservation.exception.NotAuthenticatedException;
import com.hotelreservation.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Autowired
    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/{hotelId}")
    public ResponseEntity<FavoriteResponse> toggleFavorite(
            @PathVariable Long hotelId,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
            throw new NotAuthenticatedException("User not authenticated");
        }

        String userEmail = authentication.getName();
        return ResponseEntity.ok(favoriteService.toggleFavorite(userEmail, hotelId));
    }

    @GetMapping("/status/{hotelId}")
    public ResponseEntity<FavoriteResponse> checkFavoriteStatus(
            @PathVariable Long hotelId,
            Authentication authentication) {

        boolean isFavorite = false;
        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser")) {
            String userEmail = authentication.getName();
            isFavorite = favoriteService.isFavorite(userEmail, hotelId);
        }

        return ResponseEntity.ok(FavoriteResponse.builder()
                .hotelId(hotelId)
                .isFavorite(isFavorite)
                .build());
    }
}
