package com.hotelreservation.service;

import com.hotelreservation.dto.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final Logger logger = LoggerFactory.getLogger(AiChatService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, String> sessionContexts = new HashMap<>();

    @Value("${ollama.api-url:http://localhost:11434}")
    private String ollamaApiUrl;

    @Value("${ollama.model:llama2}")
    private String ollamaModel;

    @Value("${ollama.enabled:false}")
    private boolean ollamaEnabled;

    public ChatResponse getResponse(String message) {
        return getResponse(message, null);
    }

    public ChatResponse getResponse(String message, String sessionId) {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }

        // Get or create context for this session
        String context = sessionContexts.getOrDefault(sessionId, getInitialContext());

        // Append user message to context
        context += "\nUser: " + message + "\nAssistant: ";

        // Get AI response
        String aiResponse;
        if (ollamaEnabled) {
            // Call Ollama API
            aiResponse = callOllamaApi(context);
        } else {
            // Use fallback responses
            aiResponse = getFallbackResponse(message);
        }

        // Update context with AI response
        context += aiResponse;
        sessionContexts.put(sessionId, context);

        return ChatResponse.builder()
                .message(aiResponse)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .isAiResponse(true)
                .build();
    }

    private String getInitialContext() {
        return "You are a helpful assistant for a hotel booking platform called PFA Hotels. " +
                "You can help users find hotels, answer questions about bookings, " +
                "and provide information about amenities and services. " +
                "Keep your responses concise, friendly, and helpful. " +
                "You cannot make bookings directly, but you can guide users to the booking page. " +
                "The current date is " + LocalDateTime.now().toLocalDate() + ".";
    }

    private String callOllamaApi(String prompt) {
        try {
            logger.info("Calling Ollama API with prompt: {}", prompt.substring(0, Math.min(50, prompt.length())) + "...");

            String generateUrl = ollamaApiUrl + "/api/generate";

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

            return "I'm sorry, I couldn't process your request at the moment.";
        } catch (Exception e) {
            logger.error("Error calling Ollama API: {}", e.getMessage(), e);
            return "I'm sorry, I'm having trouble connecting to my knowledge base. Please try again later.";
        }
    }

    private String getFallbackResponse(String message) {
        message = message.toLowerCase();

        // Check for common questions and provide canned responses
        if (message.contains("hello") || message.contains("hi") || message.contains("hey")) {
            return "Hello! How can I help you with your hotel search today?";
        } else if (message.contains("thank")) {
            return "You're welcome! Is there anything else I can help you with?";
        } else if (message.contains("bye") || message.contains("goodbye")) {
            return "Goodbye! Feel free to chat again if you need any help with your hotel booking.";
        } else if (message.contains("help")) {
            return "I can help you find hotels, provide information about amenities, explain booking procedures, and answer questions about our services. What would you like to know?";
        } else if (message.contains("cancel") && (message.contains("booking") || message.contains("reservation"))) {
            return "To cancel a booking, please go to your account page, find the booking in your list, and click the 'Cancel' button. Cancellation policies vary depending on the hotel and rate type.";
        } else if (message.contains("payment") && message.contains("method")) {
            return "We accept various payment methods including credit cards (Visa, MasterCard, American Express), PayPal, and bank transfers. The available payment methods will be shown during the checkout process.";
        } else if (message.contains("check-in") || message.contains("checkout") || message.contains("check out")) {
            return "Standard check-in time is usually from 2:00 PM to 3:00 PM, and check-out is typically by 11:00 AM or 12:00 PM. However, these times can vary by hotel. The exact times will be shown on your booking confirmation.";
        } else if (message.contains("breakfast")) {
            return "Breakfast options vary by hotel. Some hotels offer complimentary breakfast, while others may charge extra. You can see if breakfast is included in the room details when booking.";
        } else if (message.contains("pet") || message.contains("dog") || message.contains("cat")) {
            return "Pet policies vary by hotel. Some hotels are pet-friendly, while others may not allow pets or may charge an additional fee. You can use the 'Pet-friendly' filter when searching for hotels.";
        } else if (message.contains("wifi") || message.contains("internet")) {
            return "Most of our hotels offer complimentary WiFi. You can check if WiFi is available in the amenities section of the hotel details page.";
        } else if (message.contains("parking")) {
            return "Parking availability and fees vary by hotel, especially in city centers. You can find parking information in the amenities section of the hotel details page.";
        } else {
            return "I understand you're asking about " + message.substring(0, Math.min(20, message.length())) + "... To get the most accurate information, I recommend browsing our hotel listings or using the search function to find specific details.";
        }
    }
}
