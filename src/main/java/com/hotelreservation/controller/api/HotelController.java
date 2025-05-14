package com.hotelreservation.controller.api;

import com.hotelreservation.dto.request.HotelRequest;
import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.service.HotelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    @GetMapping
    public ResponseEntity<Page<HotelResponse>> getAllHotels(Pageable pageable) {
        return ResponseEntity.ok(hotelService.getAllHotels(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<HotelResponse>> searchHotels(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Short minRating,
            @RequestParam(required = false) Short maxRating,
            Pageable pageable) {
        return ResponseEntity.ok(hotelService.getHotelsByFilters(city, country, minRating, maxRating, pageable));
    }

    @GetMapping("/amenities")
    public ResponseEntity<Page<HotelResponse>> getHotelsByAmenities(
            @RequestParam List<Long> amenityIds,
            Pageable pageable) {
        return ResponseEntity.ok(hotelService.getHotelsByAmenities(amenityIds, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HotelResponse> getHotelById(@PathVariable Long id) {
        return ResponseEntity.ok(hotelService.getHotelById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('HOTEL_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<HotelResponse> createHotel(@Valid @RequestBody HotelRequest request) {
        return ResponseEntity.ok(hotelService.createHotel(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HOTEL_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<HotelResponse> updateHotel(
            @PathVariable Long id,
            @Valid @RequestBody HotelRequest request) {
        return ResponseEntity.ok(hotelService.updateHotel(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HOTEL_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteHotel(@PathVariable Long id) {
        hotelService.deleteHotel(id);
        return ResponseEntity.noContent().build();
    }
}
