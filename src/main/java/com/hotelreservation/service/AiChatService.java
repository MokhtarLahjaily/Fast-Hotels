package com.hotelreservation.service;

import com.hotelreservation.dto.response.ChatResponse;
import com.hotelreservation.dto.response.HotelResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final Logger logger = LoggerFactory.getLogger(AiChatService.class);
    private static final int MAX_HISTORY_LENGTH = 6;
    private static final int MAX_RETRIES = 2;

    private final RestTemplate restTemplate;
    private final Map<String, List<Map<String, String>>> conversationHistory = new ConcurrentHashMap<>();
    private final HotelService hotelService;
    private final BookingService bookingService;
    private final AmenityService amenityService;

    @Value("${ollama.api-url:http://localhost:11434}")
    private String ollamaApiUrl;

    @Value("${ollama.model:llama3.2:1b}")
    private String ollamaModel;

    @Value("${ollama.enabled:true}")
    private boolean ollamaEnabled;

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    @Value("${openai.enabled:false}")
    private boolean openAiEnabled;

    public ChatResponse getResponse(String message) {
        return getResponse(message, null);
    }

    public ChatResponse getResponse(String message, String sessionId) {
        sessionId = ensureSessionId(sessionId);
        initializeConversationHistory(sessionId);
        addMessageToHistory(sessionId, "user", message);

        String aiResponse = getAiResponse(message, sessionId);
        addMessageToHistory(sessionId, "assistant", aiResponse);

        return buildChatResponse(aiResponse, sessionId);
    }

    // ========== Session Management ==========

    private String ensureSessionId(String sessionId) {
        return sessionId != null ? sessionId : UUID.randomUUID().toString();
    }

    private void initializeConversationHistory(String sessionId) {
        if (!conversationHistory.containsKey(sessionId)) {
            conversationHistory.put(sessionId, new ArrayList<>());
            addMessageToHistory(sessionId, "system", getSystemPrompt());
        }
    }

    private void addMessageToHistory(String sessionId, String role, String content) {
        List<Map<String, String>> history = conversationHistory.get(sessionId);
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        history.add(message);

        trimHistoryIfNeeded(history);
    }

    private void trimHistoryIfNeeded(List<Map<String, String>> history) {
        if (history.size() > MAX_HISTORY_LENGTH + 1) {
            history.remove(1); // Keep system message, remove oldest user/assistant message
        }
    }

    // ========== AI Response Generation ==========

    private String getAiResponse(String message, String sessionId) {
        try {
            if (openAiEnabled && !openAiApiKey.isEmpty()) {
                return callOpenAiApi(sessionId);
            }
            if (ollamaEnabled) {
                return callOllamaApi(sessionId);
            }
            return getEnhancedFallbackResponse(message, sessionId);
        } catch (Exception e) {
            logger.error("Error getting AI response: {}", e.getMessage(), e);
            return getEnhancedFallbackResponse(message, sessionId);
        }
    }

    private String getSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append(
                "You are the official 'EasyStay' AI Assistant. You help users find hotels, manage bookings, and answer general travel questions.\n\n");
        prompt.append("STRICT RULES:\n");
        prompt.append(
                "1. ONLY recommend hotels from the 'AVAILABLE HOTELS' list provided below. DO NOT make up hotel names.\n");
        prompt.append(
                "2. If a user asks for a hotel not in the list, tell them you don't have that specific hotel but invitation them to search on our website.\n");
        prompt.append("3. Keep responses concise (capped at 3 short sentences).\n");
        prompt.append("4. Be helpful, professional, and friendly.\n\n");

        appendAvailableHotels(prompt);
        prompt.append("\nCurrent date: ").append(LocalDateTime.now().toLocalDate()).append(".\n");
        return prompt.toString();
    }

    private void appendAvailableHotels(StringBuilder prompt) {
        prompt.append("AVAILABLE HOTELS IN OUR DATABASE:\n");
        try {
            List<HotelResponse> topHotels = hotelService.getFeaturedHotels();
            if (topHotels != null && !topHotels.isEmpty()) {
                for (HotelResponse hotel : topHotels) {
                    prompt.append("- ").append(hotel.getName())
                            .append(" (").append(hotel.getCity()).append(", ").append(hotel.getCountry()).append("): ")
                            .append(hotel.getStarRating()).append(" stars. Min price: $").append(hotel.getMinPrice())
                            .append("\n");
                }
            } else {
                prompt.append(
                        "- We have many hotels across various cities like New York, Paris, and Dubai. Please search our catalog.\n");
            }
        } catch (Exception e) {
            prompt.append(
                    "- Our database is currently being updated. Please check back for specific recommendations.\n");
        }
    }

    // ========== OpenAI Integration ==========

    private String callOpenAiApi(String sessionId) {
        List<Map<String, String>> history = conversationHistory.get(sessionId);
        try {
            logger.info("Calling OpenAI API for session: {}", sessionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + openAiApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", history);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 150);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            Map<String, Object> response = restTemplate.postForObject(
                    "https://api.openai.com/v1/chat/completions", request, Map.class);

            return extractOpenAiResponse(response);
        } catch (Exception e) {
            logger.error("Error calling OpenAI API: {}", e.getMessage(), e);
            return getEnhancedFallbackResponse(history.get(history.size() - 1).get("content"), sessionId);
        }
    }

    private String extractOpenAiResponse(Map<String, Object> response) {
        if (response != null && response.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                Map<String, String> message = (Map<String, String>) choice.get("message");
                return message.get("content");
            }
        }
        return "I'm sorry, I couldn't process your request at the moment.";
    }

    // ========== Ollama Integration ==========

    private String callOllamaApi(String sessionId) {
        List<Map<String, String>> history = conversationHistory.get(sessionId);

        for (int retries = 0; retries <= MAX_RETRIES; retries++) {
            try {
                logger.info("Calling Ollama API for session: {}, attempt: {}", sessionId, retries + 1);

                String prompt = buildOllamaPrompt(history);
                Map<String, Object> requestBody = buildOllamaRequestBody(prompt);

                long startTime = System.currentTimeMillis();
                Map<String, Object> response = callOllamaEndpoint(requestBody);
                long endTime = System.currentTimeMillis();

                logger.info("Ollama API response time: {}ms", endTime - startTime);

                if (response != null && response.containsKey("response")) {
                    String responseText = (String) response.get("response");
                    logger.info("Response length: {} characters", responseText.length());
                    return responseText.trim();
                }
            } catch (RestClientException e) {
                logger.error("Error calling Ollama API (attempt {}): {}", retries + 1, e.getMessage());
                waitBeforeRetry();
            }
        }

        logger.warn("Ollama AI service is unreachable or slow. Falling back to local response logic.");
        return getEnhancedFallbackResponse(history.get(history.size() - 1).get("content"), sessionId);
    }

    private String buildOllamaPrompt(List<Map<String, String>> history) {
        StringBuilder prompt = new StringBuilder();
        int startIndex = Math.max(0, history.size() - 4);

        for (int i = startIndex; i < history.size(); i++) {
            Map<String, String> message = history.get(i);
            String role = message.get("role");
            String content = message.get("content");

            switch (role) {
                case "system":
                    prompt.append("System: ").append(content).append("\n");
                    break;
                case "user":
                    prompt.append("Q: ").append(content).append("\n");
                    break;
                case "assistant":
                    prompt.append("A: ").append(content).append("\n");
                    break;
                default:
                    // Handle unexpected role gracefully
                    logger.warn("Unexpected role in conversation history: {}", role);
                    prompt.append(content).append("\n");
                    break;
            }
        }

        prompt.append("A: ");
        logger.info("Prompt length: {} characters", prompt.length());
        return prompt.toString();
    }

    private Map<String, Object> buildOllamaRequestBody(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaModel);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.3);
        options.put("top_p", 0.8);
        options.put("num_predict", 50);
        options.put("repeat_penalty", 1.1);
        options.put("stop", Arrays.asList("Q:", "\n\n", "User:", "System:"));
        requestBody.put("options", options);

        return requestBody;
    }

    private Map<String, Object> callOllamaEndpoint(Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        return restTemplate.postForObject(ollamaApiUrl + "/api/generate", request, Map.class);
    }

    private void waitBeforeRetry() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // ========== Enhanced Fallback Response ==========

    private String getEnhancedFallbackResponse(String message, String sessionId) {
        message = message.toLowerCase();

        ConversationContext context = extractConversationContext(sessionId);

        // Use strategy pattern for response generation
        return new FallbackResponseStrategy(hotelService, context, message).getResponse();
    }

    private ConversationContext extractConversationContext(String sessionId) {
        List<Map<String, String>> history = conversationHistory.getOrDefault(sessionId, new ArrayList<>());
        String previousBotMessage = "";

        if (history.size() >= 3) {
            previousBotMessage = findPreviousMessage(history, "assistant");
        }

        return new ConversationContext(previousBotMessage);
    }

    private String findPreviousMessage(List<Map<String, String>> history, String role) {
        for (int i = history.size() - 2; i >= 0; i--) {
            Map<String, String> entry = history.get(i);
            if (role.equals(entry.get("role"))) {
                return entry.get("content");
            }
        }
        return "";
    }

    private ChatResponse buildChatResponse(String aiResponse, String sessionId) {
        return ChatResponse.builder()
                .message(aiResponse)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .isAiResponse(true)
                .build();
    }

    // ========== Inner Classes ==========

    private static class ConversationContext {
        final String previousBotMessage;

        ConversationContext(String previousBotMessage) {
            this.previousBotMessage = previousBotMessage;
        }
    }

    private static class FallbackResponseStrategy {
        private final HotelService hotelService;
        private final ConversationContext context;
        private final String message;

        FallbackResponseStrategy(HotelService hotelService, ConversationContext context, String message) {
            this.hotelService = hotelService;
            this.context = context;
            this.message = message;
        }

        String getResponse() {
            // Greetings
            if (isGreeting())
                return handleGreeting();

            // Hotel recommendations
            if (isHotelRecommendationRequest())
                return handleHotelRecommendation();

            // Gratitude
            if (message.contains("thank"))
                return "You're very welcome! Let me know if you need help with anything else.";

            // Farewell
            if (isFarewell())
                return "Goodbye! We look forward to seeing you at one of our hotels soon.";

            // Help request
            if (message.contains("help"))
                return "I can help you find hotels, explain amenities, or guide you through the booking process. Just ask about a city or a specific hotel!";

            // Specific queries
            if (message.contains("cancel") && (message.contains("booking") || message.contains("reservation"))) {
                return "To cancel, please visit 'My Bookings' in your profile. Note that some reservations have a 24-hour cancellation policy.";
            }
            if (message.contains("payment") || message.contains("pay")) {
                return "We accept all major credit cards and secure online payments. You can pay at the time of booking or at the hotel depending on the rate selected.";
            }
            if (message.contains("check-in") || message.contains("checkout") || message.contains("check out")) {
                return "Standard check-in is 3 PM and check-out is 11 AM. Times may vary by hotel.";
            }
            if (message.contains("breakfast")) {
                return "Breakfast options vary by hotel. Some include complimentary breakfast - check the room details when booking.";
            }
            if (message.contains("pet") || message.contains("dog") || message.contains("cat")) {
                return "Pet policies vary by hotel. Use the 'Pet-friendly' filter when searching for hotels that allow pets.";
            }
            if (message.contains("wifi") || message.contains("internet")) {
                return "Most hotels offer complimentary WiFi. Check the amenities section for each hotel.";
            }
            if (message.contains("parking")) {
                return "Parking availability varies by hotel. Check the amenities section for parking information.";
            }
            if (message.contains("price") || message.contains("cost") || message.contains("expensive")) {
                return "Hotel prices vary by location, amenities, and season. Use our price filter to find hotels within your budget.";
            }
            if (message.contains("discount") || message.contains("deal") || message.contains("offer")) {
                return "Check our homepage for current promotions and special deals!";
            }
            if (message.contains("review") || message.contains("rating")) {
                return "All hotels include verified guest reviews and ratings. You can filter by rating when searching.";
            }
            if (message.contains("location") || message.contains("area") || message.contains("near")) {
                return "Each hotel page shows its location on a map with nearby attractions and transportation options.";
            }

            return "I'm here to help with hotel bookings and questions. What specific information do you need?";
        }

        private boolean isGreeting() {
            return message.contains("hello") || message.contains("hi") || message.contains("hey");
        }

        private String handleGreeting() {
            if (!context.previousBotMessage.isEmpty()) {
                return "Hello again! I'm here if you have more questions about our hotels.";
            }
            return "Hello! I'm the EasyStay Assistant. How can I help you find the perfect hotel today?";
        }

        private boolean isHotelRecommendationRequest() {
            return message.contains("hotel") &&
                    (message.contains("recommend") || message.contains("suggest") ||
                            message.contains("which one") || message.contains("best"));
        }

        private String handleHotelRecommendation() {
            try {
                List<HotelResponse> featuredHotels = hotelService.getFeaturedHotels();
                if (featuredHotels != null && !featuredHotels.isEmpty()) {
                    HotelResponse hotel = featuredHotels.get(
                            java.util.concurrent.ThreadLocalRandom.current().nextInt(featuredHotels.size()));
                    return "I highly recommend " + hotel.getName() + " in " + hotel.getCity() + ". It has a "
                            + hotel.getStarRating() + "-star rating and starts at just $" + hotel.getMinPrice()
                            + " per night!";
                }
                return "I recommend searching our 'Featured' section on the homepage for our top-rated properties.";
            } catch (Exception e) {
                return "We have many great options! Try using our search filters to find a hotel that fits your budget and style.";
            }
        }

        private boolean isFarewell() {
            return message.contains("bye") || message.contains("goodbye");
        }
    }
}