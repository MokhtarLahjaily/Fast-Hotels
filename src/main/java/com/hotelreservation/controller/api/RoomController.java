package com.hotelreservation.controller.api;

import com.hotelreservation.dto.request.RoomRequest;
import com.hotelreservation.dto.response.RoomResponse;
import com.hotelreservation.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/api/hotels/{hotelId}/rooms")
    public ResponseEntity<Page<RoomResponse>> getRoomsByHotel(
            @PathVariable Long hotelId,
            Pageable pageable) {
        return ResponseEntity.ok(roomService.getRoomsByHotel(hotelId, pageable));
    }

    @GetMapping("/api/hotels/{hotelId}/rooms/available")
    public ResponseEntity<List<RoomResponse>> getAvailableRooms(
            @PathVariable Long hotelId,
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut,
            @RequestParam Integer guestCount) {
        return ResponseEntity.ok(roomService.getAvailableRooms(hotelId, checkIn, checkOut, guestCount));
    }

    @GetMapping("/api/hotels/{hotelId}/rooms/price")
    public ResponseEntity<Page<RoomResponse>> getRoomsByPriceRange(
            @PathVariable Long hotelId,
            @RequestParam Integer guestCount,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        return ResponseEntity.ok(roomService.getRoomsByPriceRange(hotelId, guestCount, minPrice, maxPrice, pageable));
    }

    @GetMapping("/api/rooms/{id}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @PostMapping("/api/hotels/{hotelId}/rooms")
    @PreAuthorize("hasRole('HOTEL_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<RoomResponse> createRoom(
            @PathVariable Long hotelId,
            @Valid @RequestBody RoomRequest request) {
        return ResponseEntity.ok(roomService.createRoom(hotelId, request));
    }

    @PutMapping("/api/rooms/{id}")
    @PreAuthorize("hasRole('HOTEL_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest request) {
        return ResponseEntity.ok(roomService.updateRoom(id, request));
    }

    @DeleteMapping("/api/rooms/{id}")
    @PreAuthorize("hasRole('HOTEL_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}
