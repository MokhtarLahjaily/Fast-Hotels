package com.hotelreservation.service;

import com.hotelreservation.dto.response.BookingResponse;
import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.dto.response.UserResponse;
import com.hotelreservation.exception.ResourceNotFoundException;
import com.hotelreservation.model.Hotel;
import com.hotelreservation.repository.BookingRepository;
import com.hotelreservation.repository.HotelRepository;
import com.hotelreservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final HotelService hotelService;
    private final BookingService bookingService;

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole().name())
                        .phone(user.getPhone())
                        .createdAt(user.getCreatedAt())
                        .build());
    }

    public Page<BookingResponse> getAllBookings(Pageable pageable) {
        return bookingRepository.findAll(pageable)
                .map(booking -> {
                    try {
                        return bookingService.getBookingById(booking.getId());
                    } catch (Exception e) {
                        // In case of circular dependency issues
                        return null;
                    }
                });
    }

    public Page<HotelResponse> getAllHotelsForApproval(Pageable pageable) {
        // In a real application, you might have an approval status field
        // For now, we'll just return all hotels
        return hotelRepository.findAll(pageable)
                .map(hotel -> hotelService.getHotelById(hotel.getId()));
    }

    @Transactional
    public HotelResponse approveHotel(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));

        // In a real application, you would set an approval status
        // For now, we'll just return the hotel

        return hotelService.getHotelById(hotel.getId());
    }

    @Transactional
    public HotelResponse rejectHotel(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));

        // In a real application, you would set a rejection status
        // For now, we'll just return the hotel

        return hotelService.getHotelById(hotel.getId());
    }
}
