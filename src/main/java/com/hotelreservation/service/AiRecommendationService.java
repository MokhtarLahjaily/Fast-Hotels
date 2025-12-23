package com.hotelreservation.service;

import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.model.Booking;
import com.hotelreservation.model.Hotel;
import com.hotelreservation.model.User;
import com.hotelreservation.repository.BookingRepository;
import com.hotelreservation.repository.HotelRepository;
import com.hotelreservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(AiRecommendationService.class);

    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final HotelService hotelService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ollama.api-url}")
    private String ollamaApiUrl;

    @Value("${ollama.model}")
    private String ollamaModel;

    @Value("${ollama.enabled:true}")
    private boolean ollamaEnabled;

    public List<HotelResponse> getRecommendedHotels(String location, String preferences) {
        // If Ollama is disabled, use default recommendations
        if (!ollamaEnabled) {
            logger.info("Ollama is disabled, using default recommendations");
            return getDefaultRecommendations(location);
        }

        // Get current user if authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = null;
        List<Booking> userBookings = new ArrayList<>();

        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser")) {
            userEmail = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(userEmail);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                userBookings = bookingRepository.findByUser(user, PageRequest.of(0, 5))
                        .getContent();
            }
        }

        // Build prompt for Ollama
        StringBuilder prompt = new StringBuilder();
        prompt.append("Based on the following information, recommend hotels:\n\n");

        if (location != null && !location.isEmpty()) {
            prompt.append("Location: ").append(location).append("\n");
        }

        if (preferences != null && !preferences.isEmpty()) {
            prompt.append("Preferences: ").append(preferences).append("\n");
        }

        if (!userBookings.isEmpty()) {
            prompt.append("\nPrevious bookings:\n");
            for (Booking booking : userBookings) {
                prompt.append("- ").append(booking.getRoom().getHotel().getName())
                        .append(" in ").append(booking.getRoom().getHotel().getCity())
                        .append(", ").append(booking.getRoom().getHotel().getCountry())
                        .append("\n");
            }
        }

        prompt.append("\nPlease provide a list of hotel IDs that would be good recommendations.");

        // Call Ollama API
        List<Long> recommendedHotelIds = getRecommendationsFromOllama(prompt.toString());

        // If no recommendations from AI, fallback to simple recommendations
        if (recommendedHotelIds.isEmpty()) {
            logger.info("No AI recommendations, falling back to default recommendations");
            return getDefaultRecommendations(location);
        }

        // Get hotel details for recommended IDs
        return recommendedHotelIds.stream()
                .map(id -> {
                    try {
                        return hotelService.getHotelById(id);
                    } catch (Exception e) {
                        logger.error("Error fetching hotel with ID: {}", id, e);
                        return null;
                    }
                })
                .filter(hotel -> hotel != null)
                .collect(Collectors.toList());
    }

    private List<HotelResponse> getDefaultRecommendations(String location) {
        // Fallback: Get top-rated hotels or hotels in the requested location
        if (location != null && !location.isEmpty()) {
            // Use the search method instead of findByFilters
            Pageable pageable = PageRequest.of(0, 5);
            Page<Hotel> hotels = hotelRepository.findAll(pageable);
            return hotels.stream()
                    .filter(hotel -> hotel.getCity().toLowerCase().contains(location.toLowerCase()) ||
                            hotel.getCountry().toLowerCase().contains(location.toLowerCase()))
                    .map(hotel -> hotelService.getHotelById(hotel.getId()))
                    .collect(Collectors.toList());
        } else {
            // Just get top 5 hotels
            return hotelRepository.findAll(PageRequest.of(0, 5))
                    .stream()
                    .map(hotel -> hotelService.getHotelById(hotel.getId()))
                    .collect(Collectors.toList());
        }
    }

    private List<Long> getRecommendationsFromOllama(String prompt) {
        List<Long> hotelIds = new ArrayList<>();

        // If Ollama is disabled, return empty list
        if (!ollamaEnabled) {
            return hotelIds;
        }

        try {
            String generateUrl = ollamaApiUrl + "/generate";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ollamaModel);
            requestBody.put("prompt", prompt);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            Map<String, Object> response = restTemplate.postForObject(generateUrl, request, Map.class);

            if (response != null && response.containsKey("response")) {
                String aiResponse = (String) response.get("response");

                // Parse hotel IDs from AI response
                // This is a simple implementation - in a real app, you'd want more robust parsing
                String[] lines = aiResponse.split("\n");
                for (String line : lines) {
                    // Use efficient non-regex check for digits
                    boolean containsDigit = line.chars().anyMatch(Character::isDigit);
                    if (line.contains("ID:") || line.contains("id:") || containsDigit) {
                        try {
                            // Extract numbers from the line
                            String numberStr = line.replaceAll("[^0-9]", "");
                            if (!numberStr.isEmpty()) {
                                Long hotelId = Long.parseLong(numberStr);
                                hotelIds.add(hotelId);
                            }
                        } catch (NumberFormatException e) {
                            // Skip if not a valid number
                            logger.warn("Failed to parse hotel ID from line: {}", line);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Log the error and return empty list
            logger.error("Error calling Ollama API: {}", e.getMessage());
        }

        return hotelIds;
    }

    // New method for the frontend
    public String getHotelRecommendation(Long hotelId) {
        // If Ollama is disabled, use default recommendation
        if (!ollamaEnabled) {
            return getDefaultRecommendation(hotelId);
        }

        // Get current user if authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
            return getDefaultRecommendation(hotelId);
        }

        // In a real application, you would generate a personalized recommendation
        // based on the user's preferences and the hotel's features
        try {
            String prompt = "Generate a short, personalized recommendation (1-2 sentences) for why a user would enjoy staying at this hotel. Make it sound natural and helpful.";

            String generateUrl = ollamaApiUrl + "/generate";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ollamaModel);
            requestBody.put("prompt", prompt);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            Map<String, Object> response = restTemplate.postForObject(generateUrl, request, Map.class);

            if (response != null && response.containsKey("response")) {
                return (String) response.get("response");
            }
        } catch (Exception e) {
            logger.error("Error generating recommendation: {}", e.getMessage());
        }

        return getDefaultRecommendation(hotelId);
    }


    /**
     * Get a default recommendation when AI is not available
     *
     * @param id The hotel ID
     * @return A default recommendation string
     */
    private String getDefaultRecommendation(Long id) {
        try {
            Optional<Hotel> hotelOpt = hotelRepository.findById(id);
            if (hotelOpt.isPresent()) {
                Hotel hotel = hotelOpt.get();

                // Generate a simple recommendation based on star rating
                if (hotel.getStarRating() >= 5) {
                    return "Experience luxury and exceptional service at this 5-star hotel, perfect for travelers seeking the finest accommodations in " + hotel.getCity() + ".";
                } else if (hotel.getStarRating() >= 4) {
                    return "This highly-rated hotel offers excellent amenities and comfort, making it a great choice for your stay in " + hotel.getCity() + ".";
                } else if (hotel.getStarRating() >= 3) {
                    return "Enjoy a comfortable and convenient stay at this well-located hotel in " + hotel.getCity() + ", offering good value for your money.";
                } else {
                    return "This budget-friendly option provides the essentials for a pleasant stay in " + hotel.getCity() + ".";
                }
            }
        } catch (Exception e) {
            logger.error("Error generating default recommendation", e);
        }

        return "This hotel is highly rated by our guests. Perfect for your stay!";
    }



    public String getRecommendationForHotel(Long id) {
        logger.info("Generating recommendation for hotel with ID: {}", id);

        // If Ollama is disabled, use default recommendation
        if (!ollamaEnabled) {
            return getDefaultRecommendation(id);
        }

        try {
            // Get current user if authenticated
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                    authentication.getName().equals("anonymousUser")) {
                return getDefaultRecommendation(id);
            }

            // Get hotel details
            Optional<Hotel> hotelOpt = hotelRepository.findById(id);
            if (hotelOpt.isEmpty()) {
                logger.warn("Hotel not found with ID: {}", id);
                return getDefaultRecommendation(id);
            }

            Hotel hotel = hotelOpt.get();

            // Build prompt for AI
            StringBuilder prompt = new StringBuilder();
            prompt.append("Generate a short, personalized recommendation (1-2 sentences) for why a user would enjoy staying at ")
                    .append(hotel.getName())
                    .append(" in ")
                    .append(hotel.getCity())
                    .append(", ")
                    .append(hotel.getCountry())
                    .append(". The hotel has ")
                    .append(hotel.getStarRating())
                    .append(" stars and offers the following amenities: ");

            // Add amenities to prompt
            hotel.getAmenities().forEach(amenity ->
                    prompt.append(amenity.getName()).append(", ")
            );

            prompt.append(". Make it sound natural, helpful, and highlight the hotel's best features.");

            // Call Ollama API
            String generateUrl = ollamaApiUrl + "/generate";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ollamaModel);
            requestBody.put("prompt", prompt.toString());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            try {
                Map<String, Object> response = restTemplate.postForObject(generateUrl, request, Map.class);

                if (response != null && response.containsKey("response")) {
                    String aiResponse = (String) response.get("response");
                    // Clean up the response (remove quotes, etc.)
                    aiResponse = aiResponse.replaceAll("^\"|\"$", "").trim();
                    return aiResponse;
                }
            } catch (RestClientException e) {
                logger.error("Error calling Ollama API: {}", e.getMessage());
                return getDefaultRecommendation(id);
            }

            return getDefaultRecommendation(id);
        } catch (Exception e) {
            logger.error("Error generating recommendation for hotel: {}", id, e);
            return getDefaultRecommendation(id);
        }
    }


}
