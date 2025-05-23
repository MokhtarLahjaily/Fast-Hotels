package com.hotelreservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long bookingId;
    private UserResponse user;
    private HotelResponse hotel;
    private Short rating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional fields for admin views and templates
    private String userEmail;
    private String hotelName;
    private String userName; // Added for hotel detail template
}
