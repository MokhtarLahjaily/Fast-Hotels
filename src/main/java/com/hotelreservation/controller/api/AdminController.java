package com.hotelreservation.controller.api;

import com.hotelreservation.dto.response.BookingResponse;
import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.dto.response.UserResponse;
import com.hotelreservation.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @GetMapping("/bookings")
    public ResponseEntity<Page<BookingResponse>> getAllBookings(Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllBookings(pageable));
    }

    @GetMapping("/hotels")
    public ResponseEntity<Page<HotelResponse>> getAllHotelsForApproval(Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllHotelsForApproval(pageable));
    }

    @PutMapping("/hotels/{id}/approve")
    public ResponseEntity<HotelResponse> approveHotel(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveHotel(id));
    }

    @PutMapping("/hotels/{id}/reject")
    public ResponseEntity<HotelResponse> rejectHotel(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.rejectHotel(id));
    }
}
