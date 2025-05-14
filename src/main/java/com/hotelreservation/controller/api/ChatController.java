package com.hotelreservation.controller.api;

import com.hotelreservation.dto.request.ChatRequest;
import com.hotelreservation.dto.response.ChatResponse;
import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.service.AiChatService;
import com.hotelreservation.service.HotelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final AiChatService aiChatService;
    private final HotelService hotelService;

    @PostMapping("/support")
    public ResponseEntity<ChatResponse> chatWithSupport(@Valid @RequestBody ChatRequest request) {
        logger.info("Received chat request: {}", request.getMessage());

        // Process the message to extract intents and entities
        String message = request.getMessage().toLowerCase();
        ChatResponse response = new ChatResponse();

        // Check for hotel search intent
        if (containsHotelSearchIntent(message)) {
            // Extract location from message
            String location = extractLocation(message);

            if (location != null) {
                logger.info("Extracted location: {}", location);

                // Search for hotels in the location
                Pageable pageable = PageRequest.of(0, 3); // Limit to 3 hotels for chat
                List<HotelResponse> hotels = hotelService.searchHotels(
                        location, null, null, null, null, "ratingDesc", pageable).getContent();

                if (!hotels.isEmpty()) {
                    response = aiChatService.getResponse(
                            "I found " + hotels.size() + " hotels in " + location + ". Here are some options:",
                            request.getSessionId());
                    response.setHotels(hotels);

                    // Add suggestions for follow-up questions
                    response.setSuggestions(Arrays.asList(
                            "Tell me more about " + hotels.get(0).getName(),
                            "What amenities does " + hotels.get(0).getName() + " have?",
                            "Show me more hotels in " + location
                    ));
                } else {
                    response = aiChatService.getResponse(
                            "I couldn't find any hotels in " + location + ". Would you like to try another location?",
                            request.getSessionId());

                    // Suggest popular destinations
                    response.setSuggestions(Arrays.asList(
                            "Hotels in Paris",
                            "Hotels in New York",
                            "Hotels in Tokyo",
                            "Hotels in London"
                    ));
                }
            } else {
                // No location found, ask for clarification
                response = aiChatService.getResponse(
                        "I'd be happy to help you find a hotel. Could you please specify which city or location you're interested in?",
                        request.getSessionId());
            }
        } else if (containsAmenitySearchIntent(message)) {
            // Handle amenity search intent
            List<String> amenities = extractAmenities(message);

            if (!amenities.isEmpty()) {
                response = aiChatService.getResponse(
                        "I can help you find hotels with " + String.join(", ", amenities) + ". " +
                                "Please also let me know which city you're interested in.",
                        request.getSessionId());
            } else {
                response = aiChatService.getResponse(request.getMessage(), request.getSessionId());
            }
        } else if (containsBookingIntent(message)) {
            // Handle booking intent
            response = aiChatService.getResponse(
                    "To make a booking, you'll need to select a hotel and room first, then fill out the booking form. " +
                            "I can help you find the perfect hotel. Which city are you planning to visit?",
                    request.getSessionId());

            // Add suggestions
            response.setSuggestions(Arrays.asList(
                    "Show me hotels in Paris",
                    "What documents do I need for booking?",
                    "What's your cancellation policy?"
            ));
        } else if (containsHelpIntent(message)) {
            // Handle help intent
            response = aiChatService.getResponse(
                    "I can help you with:\n" +
                            "- Finding hotels in specific locations\n" +
                            "- Information about amenities and services\n" +
                            "- Booking policies and procedures\n" +
                            "- Payment methods and cancellation policies\n\n" +
                            "What would you like to know about?",
                    request.getSessionId());

            // Add suggestions
            response.setSuggestions(Arrays.asList(
                    "Find hotels",
                    "Booking process",
                    "Cancellation policy",
                    "Payment methods"
            ));
        } else {
            // Default to AI response for other queries
            response = aiChatService.getResponse(request.getMessage(), request.getSessionId());
        }

        return ResponseEntity.ok(response);
    }

    private boolean containsHotelSearchIntent(String message) {
        List<String> searchKeywords = Arrays.asList(
                "find hotel", "search hotel", "looking for hotel", "show hotel",
                "hotels in", "hotel in", "accommodation in", "place to stay in",
                "find a place", "where to stay"
        );

        return searchKeywords.stream().anyMatch(message::contains);
    }

    private boolean containsAmenitySearchIntent(String message) {
        List<String> amenityKeywords = Arrays.asList(
                "with pool", "with spa", "with gym", "with wifi", "with breakfast",
                "with restaurant", "with parking", "with air conditioning",
                "pet friendly", "family friendly", "luxury", "budget"
        );

        return amenityKeywords.stream().anyMatch(message::contains);
    }

    private boolean containsBookingIntent(String message) {
        List<String> bookingKeywords = Arrays.asList(
                "book", "reserve", "make reservation", "booking", "reservation",
                "check availability", "available room", "book a room", "reserve a room"
        );

        return bookingKeywords.stream().anyMatch(message::contains);
    }

    private boolean containsHelpIntent(String message) {
        List<String> helpKeywords = Arrays.asList(
                "help", "how to", "how do i", "what is", "explain", "tell me about",
                "information", "guide", "assistance", "support"
        );

        return helpKeywords.stream().anyMatch(message::contains);
    }

    private String extractLocation(String message) {
        // Common cities pattern
        Pattern cityPattern = Pattern.compile("\\b(in|at|near|to)\\s+([a-z]+(?:\\s+[a-z]+)?)\\b");
        Matcher cityMatcher = cityPattern.matcher(message);

        if (cityMatcher.find()) {
            return cityMatcher.group(2);
        }

        // Try to find any capitalized words that might be locations
        Pattern capitalizedPattern = Pattern.compile("\\b([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?)\\b");
        Matcher capitalizedMatcher = capitalizedPattern.matcher(message);

        if (capitalizedMatcher.find()) {
            return capitalizedMatcher.group(1);
        }

        return null;
    }

    private List<String> extractAmenities(String message) {
        List<String> foundAmenities = new ArrayList<>();
        List<String> amenities = Arrays.asList(
                "pool", "spa", "gym", "wifi", "breakfast", "restaurant",
                "parking", "air conditioning", "pet friendly", "family friendly"
        );

        for (String amenity : amenities) {
            if (message.contains(amenity)) {
                foundAmenities.add(amenity);
            }
        }

        return foundAmenities;
    }
}
