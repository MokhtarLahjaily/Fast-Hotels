package com.hotelreservation.controller.api;

import com.hotelreservation.dto.request.BookingRequest;
import com.hotelreservation.dto.response.BookingResponse;
import com.hotelreservation.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<Page<BookingResponse>> getUserBookings(Pageable pageable) {
        return ResponseEntity.ok(bookingService.getCurrentUserBookings(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.ok(bookingService.createBooking(request));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(id));
    }

    @GetMapping("/hotel/{hotelId}")
    @PreAuthorize("hasRole('HOTEL_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Page<BookingResponse>> getBookingsByHotel(
            @PathVariable Long hotelId,
            Pageable pageable) {
        return ResponseEntity.ok(bookingService.getBookingsByHotel(hotelId, pageable));
    }
}
