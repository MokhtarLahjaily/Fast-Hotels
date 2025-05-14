package com.hotelreservation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class RoomRequest {

    @NotBlank(message = "Room name is required")
    private String name;

    private String description;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @NotNull(message = "Price per night is required")
    @Min(value = 0, message = "Price per night must be positive")
    private BigDecimal pricePerNight;

    @NotNull(message = "Room count is required")
    @Min(value = 1, message = "Room count must be at least 1")
    private Integer roomCount;

    private List<Long> amenityIds;
}
