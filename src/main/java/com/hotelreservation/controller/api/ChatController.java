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

import java.time.LocalDateTime;
import java.util.*;
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
        ChatResponse response;

        try {
            // First, get the AI response
            response = aiChatService.getResponse(request.getMessage(), request.getSessionId());

            // Check for hotel search intent to enhance with actual hotel data
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
                        // Add hotels to the response
                        response.setHotels(hotels);

                        // Add suggestions for follow-up questions
                        response.setSuggestions(Arrays.asList(
                                "Tell me more about " + hotels.get(0).getName(),
                                "What amenities does " + hotels.get(0).getName() + " have?",
                                "Show me more hotels in " + location));
                    } else {
                        // No hotels found, suggest popular destinations
                        response.setSuggestions(Arrays.asList(
                                "Hotels in Paris",
                                "Hotels in New York",
                                "Hotels in Tokyo",
                                "Hotels in London"));
                    }
                }
            } else if (containsAmenitySearchIntent(message)) {
                // Handle amenity search intent
                List<String> amenities = extractAmenities(message);

                if (!amenities.isEmpty()) {
                    // Add suggestions related to amenities
                    response.setSuggestions(Arrays.asList(
                            "Hotels with " + String.join(" and ", amenities),
                            "Luxury hotels with " + amenities.get(0),
                            "Budget-friendly hotels with " + amenities.get(0)));
                }
            } else if (containsBookingIntent(message)) {
                // Handle booking intent with suggestions
                response.setSuggestions(Arrays.asList(
                        "Show me hotels in Paris",
                        "What documents do I need for booking?",
                        "What's your cancellation policy?"));
            } else if (containsHelpIntent(message)) {
                // Handle help intent with suggestions
                response.setSuggestions(Arrays.asList(
                        "Find hotels",
                        "Booking process",
                        "Cancellation policy",
                        "Payment methods"));
            } else {
                // For other queries, provide general suggestions
                response.setSuggestions(Arrays.asList(
                        "Find hotels near me",
                        "How do I make a booking?",
                        "What amenities do your hotels offer?",
                        "Tell me about your loyalty program"));
            }
        } catch (Exception e) {
            logger.error("Error processing chat request: {}", e.getMessage(), e);

            // Fallback response in case of error
            response = ChatResponse.builder()
                    .message(
                            "I'm sorry, I'm having trouble processing your request right now. Please try again later or contact our support team for assistance.")
                    .sessionId(request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now()) // Fixed: Using LocalDateTime.now() directly
                    .isAiResponse(true)
                    .suggestions(Arrays.asList(
                            "Browse hotels",
                            "Contact support",
                            "Check booking status"))
                    .build();
        }

        return ResponseEntity.ok(response);
    }

    private boolean containsHotelSearchIntent(String message) {
        List<String> searchKeywords = Arrays.asList(
                "find hotel", "search hotel", "looking for hotel", "show hotel",
                "hotels in", "hotel in", "accommodation in", "place to stay in",
                "find a place", "where to stay", "best hotel", "recommend hotel",
                "hotel near", "lodging in", "stay in", "book hotel in");

        return searchKeywords.stream().anyMatch(message::contains);
    }

    private boolean containsAmenitySearchIntent(String message) {
        List<String> amenityKeywords = Arrays.asList(
                "with pool", "with spa", "with gym", "with wifi", "with breakfast",
                "with restaurant", "with parking", "with air conditioning",
                "pet friendly", "family friendly", "luxury", "budget", "free wifi",
                "room service", "fitness center", "business center", "conference room",
                "beach access", "ocean view", "mountain view", "city view", "balcony",
                "kitchen", "suite", "accessible", "wheelchair", "airport shuttle");

        return amenityKeywords.stream().anyMatch(message::contains);
    }

    private boolean containsBookingIntent(String message) {
        List<String> bookingKeywords = Arrays.asList(
                "book", "reserve", "make reservation", "booking", "reservation",
                "check availability", "available room", "book a room", "reserve a room",
                "check in", "check out", "cancel booking", "modify booking", "change reservation",
                "booking confirmation", "reservation details", "booking policy");

        return bookingKeywords.stream().anyMatch(message::contains);
    }

    private boolean containsHelpIntent(String message) {
        List<String> helpKeywords = Arrays.asList(
                "help", "how to", "how do i", "what is", "explain", "tell me about",
                "information", "guide", "assistance", "support", "faq", "question",
                "confused", "don't understand", "unclear", "more info", "details about");

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

        // Look for common city names directly
        List<String> commonCities = Arrays.asList(
                "new york", "los angeles", "chicago", "houston", "phoenix", "philadelphia",
                "san antonio", "san diego", "dallas", "san jose", "austin", "jacksonville",
                "fort worth", "columbus", "charlotte", "san francisco", "indianapolis",
                "seattle", "denver", "washington", "boston", "el paso", "nashville",
                "detroit", "oklahoma city", "portland", "las vegas", "memphis", "louisville",
                "baltimore", "milwaukee", "albuquerque", "tucson", "fresno", "sacramento",
                "kansas city", "mesa", "atlanta", "omaha", "colorado springs", "raleigh",
                "miami", "long beach", "virginia beach", "oakland", "minneapolis",
                "tampa", "tulsa", "arlington", "new orleans", "wichita", "cleveland",
                "bakersfield", "aurora", "anaheim", "honolulu", "santa ana", "riverside",
                "corpus christi", "lexington", "stockton", "st. louis", "saint louis",
                "pittsburgh", "saint paul", "anchorage", "cincinnati", "henderson",
                "greensboro", "plano", "newark", "lincoln", "toledo", "orlando",
                "chula vista", "jersey city", "chandler", "fort wayne", "buffalo",
                "durham", "st. petersburg", "irvine", "laredo", "lubbock", "madison",
                "gilbert", "norfolk", "reno", "winston-salem", "glendale", "hialeah",
                "garland", "scottsdale", "irving", "chesapeake", "north las vegas",
                "fremont", "baton rouge", "richmond", "boise", "san bernardino",
                "paris", "london", "tokyo", "rome", "barcelona", "amsterdam", "berlin",
                "madrid", "dubai", "singapore", "hong kong", "bangkok", "istanbul",
                "sydney", "melbourne", "toronto", "vancouver", "montreal", "mexico city",
                "rio de janeiro", "sao paulo", "buenos aires", "lima", "santiago",
                "cairo", "cape town", "nairobi", "marrakech", "dubai", "abu dhabi",
                "moscow", "st. petersburg", "kiev", "warsaw", "prague", "budapest",
                "vienna", "zurich", "geneva", "brussels", "copenhagen", "oslo",
                "stockholm", "helsinki", "reykjavik", "dublin", "edinburgh", "glasgow",
                "manchester", "liverpool", "birmingham", "lisbon", "porto", "athens",
                "thessaloniki", "milan", "florence", "venice", "naples", "munich",
                "frankfurt", "hamburg", "cologne", "marseille", "lyon", "nice",
                "seoul", "beijing", "shanghai", "taipei", "manila", "kuala lumpur",
                "jakarta", "delhi", "mumbai", "bangalore", "chennai", "kolkata",
                "auckland", "wellington", "christchurch", "queenstown", "honolulu",
                "cancun", "havana", "punta cana", "san juan", "nassau", "montego bay");

        for (String city : commonCities) {
            if (message.contains(city)) {
                return city;
            }
        }

        return null;
    }

    private List<String> extractAmenities(String message) {
        List<String> foundAmenities = new ArrayList<>();
        List<String> amenities = Arrays.asList(
                "pool", "spa", "gym", "wifi", "breakfast", "restaurant",
                "parking", "air conditioning", "pet friendly", "family friendly",
                "room service", "fitness center", "business center", "conference room",
                "beach access", "ocean view", "mountain view", "city view", "balcony",
                "kitchen", "suite", "accessible", "wheelchair", "airport shuttle",
                "free wifi", "bar", "lounge", "concierge", "laundry", "dry cleaning",
                "childcare", "kids club", "tennis court", "golf course", "casino",
                "nightclub", "entertainment", "hot tub", "sauna", "steam room",
                "massage", "beauty salon", "gift shop", "convenience store", "atm",
                "currency exchange", "tour desk", "ticket service", "car rental",
                "bicycle rental", "shuttle service", "valet parking", "electric vehicle charging");

        for (String amenity : amenities) {
            if (message.contains(amenity)) {
                foundAmenities.add(amenity);
            }
        }

        return foundAmenities;
    }
}
