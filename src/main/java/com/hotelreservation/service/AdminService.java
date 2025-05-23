package com.hotelreservation.service;

import com.hotelreservation.dto.response.BookingResponse;
import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.dto.response.ReviewResponse;
import com.hotelreservation.dto.response.RoomResponse;
import com.hotelreservation.dto.response.UserResponse;
import com.hotelreservation.exception.ResourceNotFoundException;
import com.hotelreservation.model.Hotel;
import com.hotelreservation.repository.BookingRepository;
import com.hotelreservation.repository.HotelRepository;
import com.hotelreservation.repository.ReviewRepository;
import com.hotelreservation.repository.RoomRepository;
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
    private final RoomRepository roomRepository;
    private final ReviewRepository reviewRepository;
    private final HotelService hotelService;
    private final BookingService bookingService;
    private final UserService userService;
    private final RoomService roomService;
    private final ReviewService reviewService;

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
                        // In case of circular dependency issues, create a basic response
                        return BookingResponse.builder()
                                .id(booking.getId())
                                .status(booking.getStatus().name()) // Convert enum to String
                                .checkInDate(booking.getCheckInDate())
                                .checkOutDate(booking.getCheckOutDate())
                                .totalPrice(booking.getTotalPrice())
                                .createdAt(booking.getCreatedAt())
                                .userEmail(booking.getUser() != null ? booking.getUser().getEmail() : "N/A")
                                .build();
                    }
                });
    }

    public Page<HotelResponse> getAllHotelsForApproval(Pageable pageable) {
        return hotelRepository.findAll(pageable)
                .map(hotel -> {
                    try {
                        return hotelService.getHotelById(hotel.getId());
                    } catch (Exception e) {
                        // Create a basic response if detailed mapping fails
                        HotelResponse response = HotelResponse.builder()
                                .id(hotel.getId())
                                .name(hotel.getName())
                                .description(hotel.getDescription())
                                .address(hotel.getAddress())
                                .city(hotel.getCity())
                                .country(hotel.getCountry())
                                .starRating(hotel.getStarRating())
                                .createdAt(hotel.getCreatedAt())
                                .build();

                        // Set owner name separately
                        if (hotel.getOwner() != null) {
                            response.setOwnerName(hotel.getOwner().getFirstName() + " " + hotel.getOwner().getLastName());
                        } else {
                            response.setOwnerName("N/A");
                        }

                        return response;
                    }
                });
    }

    public Page<RoomResponse> getAllRooms(Pageable pageable) {
        return roomRepository.findAll(pageable)
                .map(room -> {
                    try {
                        return roomService.getRoomById(room.getId());
                    } catch (Exception e) {
                        // Create a basic response if detailed mapping fails
                        RoomResponse response = RoomResponse.builder()
                                .id(room.getId())
                                .hotelId(room.getHotel() != null ? room.getHotel().getId() : null)
                                .name(room.getName())
                                .description(room.getDescription())
                                .capacity(room.getCapacity())
                                .pricePerNight(room.getPricePerNight())
                                .roomCount(room.getRoomCount())
                                .type(room.getType() != null ? room.getType() : "Standard")
                                .build();

                        // Set hotel name separately
                        if (room.getHotel() != null) {
                            response.setHotelName(room.getHotel().getName());
                        } else {
                            response.setHotelName("N/A");
                        }

                        return response;
                    }
                });
    }

    public Page<ReviewResponse> getAllReviews(Pageable pageable) {
        return reviewRepository.findAll(pageable)
                .map(review -> {
                    try {
                        return reviewService.getReviewById(review.getId());
                    } catch (Exception e) {
                        // Create a basic response if detailed mapping fails
                        ReviewResponse response = ReviewResponse.builder()
                                .id(review.getId())
                                .rating(review.getRating())
                                .comment(review.getComment())
                                .createdAt(review.getCreatedAt())
                                .build();

                        // Set user email separately
                        if (review.getUser() != null) {
                            response.setUserEmail(review.getUser().getEmail());
                        } else {
                            response.setUserEmail("N/A");
                        }

                        // Set hotel name separately
                        if (review.getBooking() != null &&
                                review.getBooking().getRoom() != null &&
                                review.getBooking().getRoom().getHotel() != null) {
                            response.setHotelName(review.getBooking().getRoom().getHotel().getName());
                        } else {
                            response.setHotelName("N/A");
                        }

                        return response;
                    }
                });
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
