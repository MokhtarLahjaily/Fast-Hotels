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

    // Injection du RestTemplate configuré au lieu de créer un nouveau
    private final RestTemplate restTemplate;
    private final Map<String, List<Map<String, String>>> conversationHistory = new ConcurrentHashMap<>();
    private final HotelService hotelService;
    private final BookingService bookingService;
    private final AmenityService amenityService;

    // Maximum conversation history to maintain per session
    private static final int MAX_HISTORY_LENGTH = 6; // Réduit de 10 à 6

    // Maximum retries for AI service
    private static final int MAX_RETRIES = 2;

    @Value("${ollama.api-url:http://localhost:11434}")
    private String ollamaApiUrl;

    @Value("${ollama.model:phi3:mini}")
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
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }

        // Initialize conversation history if it doesn't exist
        if (!conversationHistory.containsKey(sessionId)) {
            conversationHistory.put(sessionId, new ArrayList<>());
            // Add system message to set the context
            addMessageToHistory(sessionId, "system", getSystemPrompt());
        }

        // Add user message to history
        addMessageToHistory(sessionId, "user", message);

        // Get AI response
        String aiResponse;
        try {
            if (openAiEnabled && !openAiApiKey.isEmpty()) {
                aiResponse = callOpenAiApi(sessionId);
            } else if (ollamaEnabled) {
                aiResponse = callOllamaApi(sessionId);
            } else {
                aiResponse = getEnhancedFallbackResponse(message, sessionId);
            }
        } catch (Exception e) {
            logger.error("Error getting AI response: {}", e.getMessage(), e);
            aiResponse = getEnhancedFallbackResponse(message, sessionId);
        }

        // Add AI response to history
        addMessageToHistory(sessionId, "assistant", aiResponse);

        return ChatResponse.builder()
                .message(aiResponse)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .isAiResponse(true)
                .build();
    }

    private void addMessageToHistory(String sessionId, String role, String content) {
        List<Map<String, String>> history = conversationHistory.get(sessionId);
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        history.add(message);

        // Trim history if it exceeds maximum length
        if (history.size() > MAX_HISTORY_LENGTH + 1) { // +1 for the system message
            // Keep the system message (first one) and remove the oldest user/assistant message
            history.remove(1);
        }
    }

    private String getSystemPrompt() {
        // Prompt système simplifié pour des performances optimales
        return "You are a helpful hotel booking assistant for PFA Hotels. " +
                "Keep responses very short (1-2 sentences). " +
                "Help with hotel searches, bookings, and general info. " +
                "Current date: " + LocalDateTime.now().toLocalDate() + ".";
    }

    private String callOpenAiApi(String sessionId) {
        try {
            logger.info("Calling OpenAI API for session: {}", sessionId);

            String apiUrl = "https://api.openai.com/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + openAiApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", conversationHistory.get(sessionId));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 150); // Réponses plus courtes

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) choice.get("message");
                    return message.get("content");
                }
            }

            return "I'm sorry, I couldn't process your request at the moment.";
        } catch (Exception e) {
            logger.error("Error calling OpenAI API: {}", e.getMessage(), e);
            return getEnhancedFallbackResponse(conversationHistory.get(sessionId).get(conversationHistory.get(sessionId).size() - 1).get("content"), sessionId);
        }
    }

    private String callOllamaApi(String sessionId) {
        int retries = 0;
        while (retries <= MAX_RETRIES) {
            try {
                logger.info("Calling Ollama API for session: {}, attempt: {}", sessionId, retries + 1);

                String generateUrl = ollamaApiUrl + "/api/generate";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                // Format de prompt optimisé et simplifié
                StringBuilder prompt = new StringBuilder();
                List<Map<String, String>> history = conversationHistory.get(sessionId);

                // Seulement les 4 derniers messages pour éviter un prompt trop long
                int startIndex = Math.max(0, history.size() - 4);

                for (int i = startIndex; i < history.size(); i++) {
                    Map<String, String> message = history.get(i);
                    String role = message.get("role");
                    String content = message.get("content");

                    if ("system".equals(role)) {
                        prompt.append("System: ").append(content).append("\n");
                    } else if ("user".equals(role)) {
                        prompt.append("Q: ").append(content).append("\n");
                    } else if ("assistant".equals(role)) {
                        prompt.append("A: ").append(content).append("\n");
                    }
                }

                // Add the final assistant prompt
                prompt.append("A: ");

                // Log pour diagnostiquer la taille du prompt
                logger.info("Prompt length: {} characters", prompt.length());
                logger.info("Prompt preview: {}", prompt.toString().substring(0, Math.min(200, prompt.length())));

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", ollamaModel);
                requestBody.put("prompt", prompt.toString());
                requestBody.put("stream", false);

                // Paramètres d'optimisation agressifs pour des réponses plus rapides
                Map<String, Object> options = new HashMap<>();
                options.put("temperature", 0.3);         // Moins créatif = plus rapide
                options.put("top_p", 0.8);
                options.put("num_predict", 50);          // Réponses très courtes
                options.put("repeat_penalty", 1.1);
                options.put("stop", Arrays.asList("Q:", "\n\n", "User:", "System:")); // Arrêter plus tôt
                requestBody.put("options", options);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

                long startTime = System.currentTimeMillis();
                Map<String, Object> response = restTemplate.postForObject(generateUrl, request, Map.class);
                long endTime = System.currentTimeMillis();

                logger.info("Ollama API response time: {}ms", endTime - startTime);

                if (response != null && response.containsKey("response")) {
                    String responseText = (String) response.get("response");
                    logger.info("Response length: {} characters", responseText.length());
                    return responseText.trim();
                }

                retries++;
            } catch (RestClientException e) {
                logger.error("Error calling Ollama API (attempt {}): {}", retries + 1, e.getMessage());
                retries++;

                // Add a small delay before retrying
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // If all retries failed, use the enhanced fallback
        return getEnhancedFallbackResponse(conversationHistory.get(sessionId).get(conversationHistory.get(sessionId).size() - 1).get("content"), sessionId);
    }

    private String getEnhancedFallbackResponse(String message, String sessionId) {
        message = message.toLowerCase();

        // Get conversation context
        List<Map<String, String>> history = conversationHistory.getOrDefault(sessionId, new ArrayList<>());
        int historySize = history.size();

        // Extract previous messages for context
        String previousUserMessage = "";
        String previousBotMessage = "";

        if (historySize >= 3) {
            // Get the previous user message (excluding the current one)
            for (int i = historySize - 2; i >= 0; i--) {
                Map<String, String> entry = history.get(i);
                if ("user".equals(entry.get("role"))) {
                    previousUserMessage = entry.get("content");
                    break;
                }
            }

            // Get the previous bot message
            for (int i = historySize - 2; i >= 0; i--) {
                Map<String, String> entry = history.get(i);
                if ("assistant".equals(entry.get("role"))) {
                    previousBotMessage = entry.get("content");
                    break;
                }
            }
        }

        // Check for common questions with context awareness
        if (message.contains("hello") || message.contains("hi") || message.contains("hey")) {
            if (!previousBotMessage.isEmpty()) {
                return "Hello again! How can I help you today?";
            }
            return "Hello! How can I help you with your hotel search?";
        } else if (message.contains("thank")) {
            return "You're welcome! Anything else I can help with?";
        } else if (message.contains("bye") || message.contains("goodbye")) {
            return "Goodbye! Have a great day!";
        } else if (message.contains("help")) {
            return "I can help you find hotels, check bookings, and answer questions about our services. What do you need?";
        } else if (message.contains("cancel") && (message.contains("booking") || message.contains("reservation"))) {
            return "To cancel a booking, go to your account page and click 'Cancel' on your reservation. Most bookings can be cancelled 24 hours before check-in.";
        } else if (message.contains("payment") && message.contains("method")) {
            return "We accept credit cards, PayPal, and bank transfers. Payment options will be shown during checkout.";
        } else if (message.contains("check-in") || message.contains("checkout") || message.contains("check out")) {
            return "Standard check-in is 3 PM and check-out is 11 AM. Times may vary by hotel.";
        } else if (message.contains("breakfast")) {
            return "Breakfast options vary by hotel. Some include complimentary breakfast - check the room details when booking.";
        } else if (message.contains("pet") || message.contains("dog") || message.contains("cat")) {
            return "Pet policies vary by hotel. Use the 'Pet-friendly' filter when searching for hotels that allow pets.";
        } else if (message.contains("wifi") || message.contains("internet")) {
            return "Most hotels offer complimentary WiFi. Check the amenities section for each hotel.";
        } else if (message.contains("parking")) {
            return "Parking availability varies by hotel. Check the amenities section for parking information.";
        } else if (message.contains("hotel") && (message.contains("recommend") || message.contains("suggest"))) {
            try {
                List<HotelResponse> featuredHotels = hotelService.getFeaturedHotels();
                if (!featuredHotels.isEmpty()) {
                    HotelResponse hotel = featuredHotels.get(new Random().nextInt(featuredHotels.size()));
                    return "I recommend " + hotel.getName() + " in " + hotel.getCity() + ". It's highly rated by guests!";
                }
            } catch (Exception e) {
                logger.warn("Could not fetch featured hotels for recommendation: {}", e.getMessage());
            }
        } else if (message.contains("price") || message.contains("cost") || message.contains("expensive")) {
            return "Hotel prices vary by location, amenities, and season. Use our price filter to find hotels within your budget.";
        } else if (message.contains("discount") || message.contains("deal") || message.contains("offer")) {
            return "Check our homepage for current promotions and special deals!";
        } else if (message.contains("review") || message.contains("rating")) {
            return "All hotels include verified guest reviews and ratings. You can filter by rating when searching.";
        } else if (message.contains("location") || message.contains("area") || message.contains("near")) {
            return "Each hotel page shows its location on a map with nearby attractions and transportation options.";
        }

        // Default response - keep it short
        return "I'm here to help with hotel bookings and questions. What specific information do you need?";
    }
}