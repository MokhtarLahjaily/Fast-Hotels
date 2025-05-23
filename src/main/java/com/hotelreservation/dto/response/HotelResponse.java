package com.hotelreservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelResponse {
    private Long id;
    private String name;
    private String description;
    private String address;
    private String city;
    private String country;
    private String postalCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Short starRating;
    private Double averageRating;
    private UserResponse owner;
    private List<AmenityResponse> amenities;
    private List<ImageResponse> images;
    private String imageUrl; // For primary image URL
    private String primaryImageUrl; // Added for template compatibility
    private BigDecimal minPrice; // Added for minimum price display
    private List<RoomResponse> rooms; // Added to store room information
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional field for admin views
    private String ownerName;
}
