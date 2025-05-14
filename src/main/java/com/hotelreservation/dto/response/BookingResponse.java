package com.hotelreservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private UserResponse user;
    private RoomResponse room;
    private HotelResponse hotel;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guestCount;
    private BigDecimal totalPrice;
    private String specialRequests;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Adding these convenience properties to match what the template expects
    private String hotelName;
    private String roomType;
}
