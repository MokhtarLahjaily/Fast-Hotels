package com.hotelreservation.service;

import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.model.Favorite;
import com.hotelreservation.model.Hotel;
import com.hotelreservation.model.User;
import com.hotelreservation.repository.FavoriteRepository;
import com.hotelreservation.repository.HotelRepository;
import com.hotelreservation.repository.UserRepository;
import com.hotelreservation.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final HotelService hotelService;
    private FavoriteService self;

    public FavoriteService(FavoriteRepository favoriteRepository,
            UserRepository userRepository,
            HotelRepository hotelRepository,
            HotelService hotelService) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.hotelRepository = hotelRepository;
        this.hotelService = hotelService;
    }

    @Autowired
    public void setSelf(@Lazy FavoriteService self) {
        this.self = self;
    }

    public boolean addToFavorites(String userEmail, Long hotelId) {
        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

            Hotel hotel = hotelRepository.findById(hotelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + hotelId));

            // Check if already in favorites
            if (favoriteRepository.existsByUserAndHotel(user, hotel)) {
                log.info("Hotel {} is already in favorites for user {}", hotelId, userEmail);
                return false;
            }

            Favorite favorite = new Favorite();
            favorite.setUser(user);
            favorite.setHotel(hotel);
            favorite.setCreatedAt(LocalDateTime.now());

            favoriteRepository.save(favorite);
            log.info("Added hotel {} to favorites for user {}", hotelId, userEmail);
            return true;
        } catch (Exception e) {
            log.error("Error adding hotel {} to favorites for user {}: {}", hotelId, userEmail, e.getMessage());
            throw e;
        }
    }

    public boolean removeFromFavorites(String userEmail, Long hotelId) {
        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

            Hotel hotel = hotelRepository.findById(hotelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + hotelId));

            Optional<Favorite> favorite = favoriteRepository.findByUserAndHotel(user, hotel);
            if (favorite.isPresent()) {
                favoriteRepository.delete(favorite.get());
                log.info("Removed hotel {} from favorites for user {}", hotelId, userEmail);
                return true;
            } else {
                log.info("Hotel {} was not in favorites for user {}", hotelId, userEmail);
                return false;
            }
        } catch (Exception e) {
            log.error("Error removing hotel {} from favorites for user {}: {}", hotelId, userEmail, e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(String userEmail, Long hotelId) {
        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElse(null);

            if (user == null) {
                return false;
            }

            Hotel hotel = hotelRepository.findById(hotelId)
                    .orElse(null);

            if (hotel == null) {
                return false;
            }

            return favoriteRepository.existsByUserAndHotel(user, hotel);
        } catch (Exception e) {
            log.error("Error checking if hotel {} is favorite for user {}: {}", hotelId, userEmail, e.getMessage());
            return false;
        }
    }

    @Transactional(readOnly = true)
    public Page<Hotel> getUserFavoriteHotels(String userEmail, Pageable pageable) {
        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

            return favoriteRepository.findHotelsByUser(user, pageable);
        } catch (Exception e) {
            log.error("Error getting favorites for user {}: {}", userEmail, e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<Hotel> getUserFavoriteHotelsList(String userEmail) {
        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

            return favoriteRepository.findHotelsByUser(user);
        } catch (Exception e) {
            log.error("Error getting favorites list for user {}: {}", userEmail, e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<HotelResponse> getUserFavorites(String userEmail) {
        try {
            List<Hotel> favoriteHotels = self.getUserFavoriteHotelsList(userEmail);

            if (favoriteHotels.isEmpty()) {
                return new ArrayList<>();
            }

            return favoriteHotels.stream()
                    .map(hotel -> hotelService.getHotelById(hotel.getId()))
                    .toList();
        } catch (Exception e) {
            log.error("Error getting favorite hotel responses for user {}: {}", userEmail, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public long getUserFavoritesCount(String userEmail) {
        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElse(null);

            if (user == null) {
                return 0;
            }

            return favoriteRepository.countByUser(user);
        } catch (Exception e) {
            log.error("Error getting favorites count for user {}: {}", userEmail, e.getMessage());
            return 0;
        }
    }

    // Convenience methods that get user email from security context
    public boolean isFavorite(Long hotelId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
            return false;
        }
        return self.isFavorite(authentication.getName(), hotelId);
    }

    public void addFavorite(Long hotelId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
            throw new RuntimeException("User not authenticated");
        }
        self.addToFavorites(authentication.getName(), hotelId);
    }

    public void removeFavorite(Long hotelId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
            throw new RuntimeException("User not authenticated");
        }
        self.removeFromFavorites(authentication.getName(), hotelId);
    }

    @Transactional
    public com.hotelreservation.dto.response.FavoriteResponse toggleFavorite(String userEmail, Long hotelId) {
        boolean isFav = isFavorite(userEmail, hotelId);
        String status;
        String message;

        if (isFav) {
            removeFromFavorites(userEmail, hotelId);
            status = "removed";
            message = "Hotel removed from favorites";
        } else {
            addToFavorites(userEmail, hotelId);
            status = "added";
            message = "Hotel added to favorites";
        }

        return com.hotelreservation.dto.response.FavoriteResponse.builder()
                .status(status)
                .message(message)
                .hotelId(hotelId)
                .isFavorite(!isFav)
                .build();
    }
}
