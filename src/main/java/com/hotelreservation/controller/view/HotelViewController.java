package com.hotelreservation.controller.view;

import com.hotelreservation.dto.response.AmenityResponse;
import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.dto.response.ImageResponse;
import com.hotelreservation.dto.response.ReviewResponse;
import com.hotelreservation.dto.response.RoomResponse;
import com.hotelreservation.service.AiRecommendationService;
import com.hotelreservation.service.AmenityService;
import com.hotelreservation.service.FavoriteService;
import com.hotelreservation.service.HotelService;
import com.hotelreservation.service.ImageService;
import com.hotelreservation.service.ReviewService;
import com.hotelreservation.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import com.hotelreservation.util.LogSanitizer;

@Controller
public class HotelViewController {

    private final HotelService hotelService;
    private final RoomService roomService;
    private final ReviewService reviewService;
    private final AmenityService amenityService;
    private final AiRecommendationService aiRecommendationService;
    private final FavoriteService favoriteService;
    private final ImageService imageService;

    private static final Logger logger = LoggerFactory.getLogger(HotelViewController.class);

    @Autowired
    public HotelViewController(HotelService hotelService, RoomService roomService,
            ReviewService reviewService, AmenityService amenityService,
            AiRecommendationService aiRecommendationService,
            FavoriteService favoriteService, ImageService imageService) {
        this.hotelService = hotelService;
        this.roomService = roomService;
        this.reviewService = reviewService;
        this.amenityService = amenityService;
        this.aiRecommendationService = aiRecommendationService;
        this.favoriteService = favoriteService;
        this.imageService = imageService;
    }

    @GetMapping("/hotels")
    public String listHotels(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) List<Integer> starRating,
            @RequestParam(required = false) List<Long> amenityIds,
            @RequestParam(required = false, defaultValue = "recommended") String sortBy,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            Model model) {

        try {
            // Log search parameters
            logger.info("Listing hotels with destination: {}, sortBy: {}, page: {}, size: {}",
                    LogSanitizer.sanitize(destination), LogSanitizer.sanitize(sortBy), page, size);

            // Get hotels with filters
            Page<HotelResponse> hotelsPage = hotelService.searchHotels(
                    destination, minPrice, maxPrice, starRating, amenityIds, sortBy, PageRequest.of(page, size));

            // Get all amenities for filter options
            List<AmenityResponse> amenities = amenityService.getAllAmenities();

            model.addAttribute("hotels", hotelsPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", hotelsPage.getTotalPages());
            model.addAttribute("totalHotels", hotelsPage.getTotalElements());
            model.addAttribute("size", size);
            model.addAttribute("amenities", amenities);
            model.addAttribute("searchQuery", destination);
            model.addAttribute("destination", destination);
            model.addAttribute("minPrice", minPrice);
            model.addAttribute("maxPrice", maxPrice);
            model.addAttribute("starRating", starRating);
            model.addAttribute("sortBy", sortBy);

            return "hotel/list";
        } catch (Exception e) {
            logger.error("Error listing hotels", e);
            model.addAttribute("errorMessage", "An error occurred while searching for hotels. Please try again.");
            model.addAttribute("hotels", new ArrayList<>());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("size", size);
            model.addAttribute("amenities", new ArrayList<>());
            return "hotel/list";
        }
    }

    @GetMapping("/hotels/{id}")
    public String viewHotel(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate checkIn,
            @RequestParam(required = false) LocalDate checkOut,
            @RequestParam(required = false, defaultValue = "2") Integer guests,
            @RequestParam(required = false, defaultValue = "0") int reviewPage,
            @RequestParam(required = false, defaultValue = "5") int reviewSize,
            HttpServletRequest request,
            Authentication authentication,
            Model model) {

        try {
            logger.info("Viewing hotel with ID: {}", id);

            // Get hotel details
            HotelResponse hotel = hotelService.getHotelById(id);

            // Get all hotel images
            List<ImageResponse> hotelImages = imageService.getHotelImages(id);
            logger.info("Found {} images for hotel {}", hotelImages.size(), id);

            // Get reviews with pagination
            Page<ReviewResponse> reviewsPage = reviewService.getHotelReviews(id,
                    PageRequest.of(reviewPage, reviewSize));

            // Default dates if not provided
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);

            checkIn = checkIn != null ? checkIn : today;
            checkOut = checkOut != null ? checkOut : tomorrow;

            // Get available rooms for the selected dates
            List<RoomResponse> availableRooms = roomService.getAvailableRooms(id, checkIn, checkOut, guests);

            // Get AI recommendation if user is authenticated
            String aiRecommendation = aiRecommendationService.getHotelRecommendation(id);

            // Check if hotel is in user's favorites
            boolean isFavorite = false;
            if (authentication != null && authentication.isAuthenticated() &&
                    !authentication.getName().equals("anonymousUser")) {
                isFavorite = favoriteService.isFavorite(id);
            }

            // Create the current URL for redirect after favorite actions
            String currentUrl = request.getRequestURI();
            if (request.getQueryString() != null) {
                currentUrl += "?" + request.getQueryString();
            }

            model.addAttribute("hotel", hotel);
            model.addAttribute("hotelImages", hotelImages); // Add all hotel images
            model.addAttribute("reviews", reviewsPage.getContent());
            model.addAttribute("reviewCount", reviewsPage.getTotalElements());
            model.addAttribute("reviewCurrentPage", reviewPage);
            model.addAttribute("reviewTotalPages", reviewsPage.getTotalPages());
            model.addAttribute("availableRooms", availableRooms);
            model.addAttribute("aiRecommendation", aiRecommendation);
            model.addAttribute("checkIn", checkIn);
            model.addAttribute("checkOut", checkOut);
            model.addAttribute("guests", guests);
            model.addAttribute("isFavorite", isFavorite);
            model.addAttribute("currentUrl", currentUrl);

            return "hotel/detail";
        } catch (Exception e) {
            logger.error("Error viewing hotel with ID: {}", id, e);
            model.addAttribute("errorMessage", "An error occurred while loading the hotel details. Please try again.");
            return "hotel/detail";
        }
    }
}