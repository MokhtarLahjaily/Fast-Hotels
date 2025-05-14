package com.hotelreservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String message;
    private String sessionId;
    private LocalDateTime timestamp;
    private boolean isAiResponse;
    private List<HotelResponse> hotels;
    private List<String> suggestions;
}
