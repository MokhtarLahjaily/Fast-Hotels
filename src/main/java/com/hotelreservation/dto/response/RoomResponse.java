package com.hotelreservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {
    private Long id;
    private Long hotelId;
    private String name;
    private String description;
    private Integer capacity;
    private BigDecimal pricePerNight;
    private Integer roomCount;
    private List<AmenityResponse> amenities;
    private List<ImageResponse> images;
    private Integer availableCount; // Added missing property
    private String type;

    // Additional fields for admin views
    private String hotelName;
}
