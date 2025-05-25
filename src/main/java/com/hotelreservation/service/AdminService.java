package com.hotelreservation.service;

import com.hotelreservation.dto.response.BookingResponse;
import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.dto.response.ReviewResponse;
import com.hotelreservation.dto.response.RoomResponse;
import com.hotelreservation.dto.response.UserResponse;
import com.hotelreservation.exception.ResourceNotFoundException;
import com.hotelreservation.model.Booking;
import com.hotelreservation.model.BookingStatus;
import com.hotelreservation.model.Hotel;
import com.hotelreservation.model.Room;
import com.hotelreservation.repository.BookingRepository;
import com.hotelreservation.repository.HotelRepository;
import com.hotelreservation.repository.ReviewRepository;
import com.hotelreservation.repository.RoomRepository;
import com.hotelreservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
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

    public Page<UserResponse> getAllUsers(Pageable pageable, String search, String role, String status) {
        return userRepository.findUsersWithFilters(search, role, status, pageable)
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

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return getAllUsers(pageable, null, null, null);
    }

    public Page<BookingResponse> getAllBookings(Pageable pageable, String search, String status, String dateRange) {
        log.info("Getting all bookings with filters - search: {}, status: {}, dateRange: {}", search, status, dateRange);

        try {
            // Convert string status to enum
            BookingStatus statusEnum = null;
            if (status != null && !status.isEmpty()) {
                try {
                    statusEnum = BookingStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status: {}", status);
                }
            }

            // Calculate start date based on date range
            LocalDateTime startDate = null;
            if (dateRange != null && !dateRange.isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                switch (dateRange.toLowerCase()) {
                    case "today":
                        startDate = now.toLocalDate().atStartOfDay();
                        break;
                    case "week":
                        startDate = now.minusDays(7);
                        break;
                    case "month":
                        startDate = now.minusDays(30);
                        break;
                }
            }

            Page<Booking> bookingsPage;

            // Use different repository methods based on available filters
            if (search != null && !search.isEmpty() && statusEnum != null && startDate != null) {
                bookingsPage = bookingRepository.findByIdOrUserEmailContainingAndStatusAndCreatedAtAfter(search, statusEnum, startDate, pageable);
            } else if (search != null && !search.isEmpty() && statusEnum != null) {
                bookingsPage = bookingRepository.findByIdOrUserEmailContainingAndStatus(search, statusEnum, pageable);
            } else if (search != null && !search.isEmpty() && startDate != null) {
                bookingsPage = bookingRepository.findByIdOrUserEmailContainingAndCreatedAtAfter(search, startDate, pageable);
            } else if (statusEnum != null && startDate != null) {
                bookingsPage = bookingRepository.findByStatusAndCreatedAtAfter(statusEnum, startDate, pageable);
            } else if (search != null && !search.isEmpty()) {
                bookingsPage = bookingRepository.findByIdOrUserEmailContaining(search, pageable);
            } else if (statusEnum != null) {
                bookingsPage = bookingRepository.findByStatus(statusEnum, pageable);
            } else if (startDate != null) {
                bookingsPage = bookingRepository.findByCreatedAtAfter(startDate, pageable);
            } else {
                // No filters, get all bookings
                bookingsPage = bookingRepository.findAll(pageable);
            }

            log.info("Found {} bookings", bookingsPage.getTotalElements());

            return bookingsPage.map(booking -> {
                try {
                    // Use admin-specific method that bypasses authorization
                    return bookingService.getBookingByIdForAdmin(booking.getId());
                } catch (Exception e) {
                    log.warn("Error getting booking details for ID {}: {}", booking.getId(), e.getMessage());

                    // Create a comprehensive fallback response with user information
                    UserResponse userResponse = null;
                    if (booking.getUser() != null) {
                        userResponse = UserResponse.builder()
                                .id(booking.getUser().getId())
                                .email(booking.getUser().getEmail())
                                .firstName(booking.getUser().getFirstName())
                                .lastName(booking.getUser().getLastName())
                                .role(booking.getUser().getRole() != null ? booking.getUser().getRole().name() : "CUSTOMER")
                                .phone(booking.getUser().getPhone())
                                .createdAt(booking.getUser().getCreatedAt())
                                .build();
                    }

                    HotelResponse hotelResponse = null;
                    if (booking.getRoom() != null && booking.getRoom().getHotel() != null) {
                        hotelResponse = HotelResponse.builder()
                                .id(booking.getRoom().getHotel().getId())
                                .name(booking.getRoom().getHotel().getName())
                                .address(booking.getRoom().getHotel().getAddress())
                                .city(booking.getRoom().getHotel().getCity())
                                .country(booking.getRoom().getHotel().getCountry())
                                .starRating(booking.getRoom().getHotel().getStarRating())
                                .build();
                    }

                    RoomResponse roomResponse = null;
                    if (booking.getRoom() != null) {
                        roomResponse = RoomResponse.builder()
                                .id(booking.getRoom().getId())
                                .name(booking.getRoom().getName())
                                .type(booking.getRoom().getType())
                                .capacity(booking.getRoom().getCapacity())
                                .pricePerNight(booking.getRoom().getPricePerNight())
                                .hotelId(booking.getRoom().getHotel() != null ? booking.getRoom().getHotel().getId() : null)
                                .hotelName(booking.getRoom().getHotel() != null ? booking.getRoom().getHotel().getName() : "N/A")
                                .build();
                    }

                    return BookingResponse.builder()
                            .id(booking.getId())
                            .user(userResponse)
                            .room(roomResponse)
                            .hotel(hotelResponse)
                            .checkInDate(booking.getCheckInDate())
                            .checkOutDate(booking.getCheckOutDate())
                            .guestCount(booking.getGuestCount())
                            .totalPrice(booking.getTotalPrice())
                            .specialRequests(booking.getSpecialRequests())
                            .status(booking.getStatus().name())
                            .createdAt(booking.getCreatedAt())
                            .updatedAt(booking.getUpdatedAt())
                            // Convenience fields for templates
                            .userEmail(booking.getUser() != null ? booking.getUser().getEmail() : "N/A")
                            .hotelName(booking.getRoom() != null && booking.getRoom().getHotel() != null ?
                                    booking.getRoom().getHotel().getName() : "N/A")
                            .roomName(booking.getRoom() != null ? booking.getRoom().getName() : "N/A")
                            .roomType(booking.getRoom() != null ? booking.getRoom().getType() : "N/A")
                            .build();
                }
            });
        } catch (Exception e) {
            log.error("Error getting bookings: {}", e.getMessage(), e);
            throw e;
        }
    }

    public Page<BookingResponse> getAllBookings(Pageable pageable) {
        return getAllBookings(pageable, null, null, null);
    }

    public Page<HotelResponse> getAllHotelsForApproval(Pageable pageable, String search, String city, String status) {
        return hotelRepository.findHotelsWithFilters(search, city, status, pageable)
                .map(hotel -> {
                    try {
                        return hotelService.getHotelById(hotel.getId());
                    } catch (Exception e) {
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

                        if (hotel.getOwner() != null) {
                            response.setOwnerName(hotel.getOwner().getFirstName() + " " + hotel.getOwner().getLastName());
                        } else {
                            response.setOwnerName("N/A");
                        }

                        return response;
                    }
                });
    }

    public Page<HotelResponse> getAllHotelsForApproval(Pageable pageable) {
        return getAllHotelsForApproval(pageable, null, null, null);
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

    public Page<RoomResponse> getAllRooms(Pageable pageable, Long hotelId, String roomType, String status) {
        log.info("Getting rooms with filters - hotelId: {}, roomType: {}, status: {}", hotelId, roomType, status);

        Page<Room> roomsPage = roomRepository.findRoomsWithFilters(hotelId, roomType, pageable);

        return roomsPage.map(room -> {
            try {
                return roomService.getRoomById(room.getId());
            } catch (Exception e) {
                log.warn("Error getting room details for ID {}: {}", room.getId(), e.getMessage());

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

    public Page<ReviewResponse> getAllReviews(Pageable pageable, Long hotelId, Integer rating, String status) {
        return reviewRepository.findReviewsWithFilters(hotelId, rating, status, pageable)
                .map(review -> {
                    try {
                        return reviewService.getReviewById(review.getId());
                    } catch (Exception e) {
                        ReviewResponse response = ReviewResponse.builder()
                                .id(review.getId())
                                .rating(review.getRating())
                                .comment(review.getComment())
                                .createdAt(review.getCreatedAt())
                                .build();

                        if (review.getUser() != null) {
                            response.setUserEmail(review.getUser().getEmail());
                            response.setUser(UserResponse.builder()
                                    .id(review.getUser().getId())
                                    .firstName(review.getUser().getFirstName())
                                    .lastName(review.getUser().getLastName())
                                    .email(review.getUser().getEmail())
                                    .build());
                        }

                        if (review.getBooking() != null &&
                                review.getBooking().getRoom() != null &&
                                review.getBooking().getRoom().getHotel() != null) {
                            response.setHotelName(review.getBooking().getRoom().getHotel().getName());
                            response.setHotel(HotelResponse.builder()
                                    .id(review.getBooking().getRoom().getHotel().getId())
                                    .name(review.getBooking().getRoom().getHotel().getName())
                                    .build());
                        }

                        return response;
                    }
                });
    }

    public Page<ReviewResponse> getAllReviews(Pageable pageable) {
        return getAllReviews(pageable, null, null, null);
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
