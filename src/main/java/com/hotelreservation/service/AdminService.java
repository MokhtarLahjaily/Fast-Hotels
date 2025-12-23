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
import com.hotelreservation.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
                .map(this::mapToUserResponse);
    }

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return getAllUsers(pageable, null, null, null);
    }

    public Page<BookingResponse> getAllBookings(Pageable pageable, String search, String status, String dateRange) {
        log.info("Getting all bookings with filters - search: {}, status: {}, dateRange: {}", search, status,
                dateRange);

        try {
            BookingStatus statusEnum = parseBookingStatus(status);
            LocalDateTime startDate = calculateStartDate(dateRange);
            Page<Booking> bookingsPage = fetchBookingsWithFilters(search, statusEnum, startDate, pageable);

            log.info("Found {} bookings", bookingsPage.getTotalElements());
            return bookingsPage.map(this::mapToBookingResponse);
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
                .map(this::mapToHotelResponseSafely);
    }

    public Page<HotelResponse> getAllHotelsForApproval(Pageable pageable) {
        return getAllHotelsForApproval(pageable, null, null, null);
    }

    public Page<RoomResponse> getAllRooms(Pageable pageable) {
        return roomRepository.findAll(pageable)
                .map(this::mapToRoomResponseSafely);
    }

    public Page<RoomResponse> getAllRooms(Pageable pageable, Long hotelId, String roomType, String status) {
        log.info("Getting rooms with filters - hotelId: {}, roomType: {}, status: {}", hotelId, roomType, status);
        Page<Room> roomsPage = roomRepository.findRoomsWithFilters(hotelId, roomType, pageable);
        return roomsPage.map(this::mapToRoomResponseSafely);
    }

    public Page<ReviewResponse> getAllReviews(Pageable pageable, Long hotelId, Integer rating, String status) {
        return reviewRepository.findReviewsWithFilters(hotelId, rating, status, pageable)
                .map(this::mapToReviewResponseSafely);
    }

    public Page<ReviewResponse> getAllReviews(Pageable pageable) {
        return getAllReviews(pageable, null, null, null);
    }

    @Transactional
    public HotelResponse approveHotel(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.MSG_ERR_HOTEL_NOT_FOUND));
        return hotelService.getHotelById(hotel.getId());
    }

    @Transactional
    public HotelResponse rejectHotel(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.MSG_ERR_HOTEL_NOT_FOUND));
        return hotelService.getHotelById(hotel.getId());
    }

    // ========== Private Helper Methods ==========

    private UserResponse mapToUserResponse(com.hotelreservation.model.User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private BookingStatus parseBookingStatus(String status) {
        if (status == null || status.isEmpty()) {
            return null;
        }

        try {
            return BookingStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status: {}", status);
            return null;
        }
    }

    private LocalDateTime calculateStartDate(String dateRange) {
        if (dateRange == null || dateRange.isEmpty()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        switch (dateRange.toLowerCase()) {
            case "today":
                return now.toLocalDate().atStartOfDay();
            case "week":
                return now.minusDays(7);
            case "month":
                return now.minusDays(30);
            default:
                return null;
        }
    }

    private Page<Booking> fetchBookingsWithFilters(String search, BookingStatus statusEnum,
            LocalDateTime startDate, Pageable pageable) {
        boolean hasSearch = search != null && !search.isEmpty();
        boolean hasStatus = statusEnum != null;
        boolean hasDate = startDate != null;

        // All three filters
        if (hasSearch && hasStatus && hasDate) {
            return bookingRepository.findByIdOrUserEmailContainingAndStatusAndCreatedAtAfter(
                    search, statusEnum, startDate, pageable);
        }

        // Two filters combinations
        if (hasSearch && hasStatus) {
            return bookingRepository.findByIdOrUserEmailContainingAndStatus(search, statusEnum, pageable);
        }
        if (hasSearch && hasDate) {
            return bookingRepository.findByIdOrUserEmailContainingAndCreatedAtAfter(search, startDate, pageable);
        }
        if (hasStatus && hasDate) {
            return bookingRepository.findByStatusAndCreatedAtAfter(statusEnum, startDate, pageable);
        }

        // Single filter
        if (hasSearch) {
            return bookingRepository.findByIdOrUserEmailContaining(search, pageable);
        }
        if (hasStatus) {
            return bookingRepository.findByStatus(statusEnum, pageable);
        }
        if (hasDate) {
            return bookingRepository.findByCreatedAtAfter(startDate, pageable);
        }

        // No filters
        return bookingRepository.findAll(pageable);
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        try {
            return bookingService.getBookingByIdForAdmin(booking.getId());
        } catch (Exception e) {
            log.warn("Error getting booking details for ID {}: {}", booking.getId(), e.getMessage());
            return createFallbackBookingResponse(booking);
        }
    }

    private BookingResponse createFallbackBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .user(buildUserResponse(booking))
                .room(buildRoomResponse(booking))
                .hotel(buildHotelResponse(booking))
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .guestCount(booking.getGuestCount())
                .totalPrice(booking.getTotalPrice())
                .specialRequests(booking.getSpecialRequests())
                .status(booking.getStatus().name())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .userEmail(getUserEmail(booking))
                .hotelName(getHotelName(booking))
                .roomName(getRoomName(booking))
                .roomType(getRoomType(booking))
                .build();
    }

    private UserResponse buildUserResponse(Booking booking) {
        if (booking.getUser() == null) {
            return null;
        }

        return UserResponse.builder()
                .id(booking.getUser().getId())
                .email(booking.getUser().getEmail())
                .firstName(booking.getUser().getFirstName())
                .lastName(booking.getUser().getLastName())
                .role(booking.getUser().getRole() != null ? booking.getUser().getRole().name() : "CUSTOMER")
                .phone(booking.getUser().getPhone())
                .createdAt(booking.getUser().getCreatedAt())
                .build();
    }

    private HotelResponse buildHotelResponse(Booking booking) {
        if (booking.getRoom() == null || booking.getRoom().getHotel() == null) {
            return null;
        }

        Hotel hotel = booking.getRoom().getHotel();
        return HotelResponse.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .address(hotel.getAddress())
                .city(hotel.getCity())
                .country(hotel.getCountry())
                .starRating(hotel.getStarRating())
                .build();
    }

    private RoomResponse buildRoomResponse(Booking booking) {
        if (booking.getRoom() == null) {
            return null;
        }

        Room room = booking.getRoom();
        return RoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .type(room.getType())
                .capacity(room.getCapacity())
                .pricePerNight(room.getPricePerNight())
                .hotelId(room.getHotel() != null ? room.getHotel().getId() : null)
                .hotelName(room.getHotel() != null ? room.getHotel().getName() : Constants.DEFAULT_NA)
                .build();
    }

    private String getUserEmail(Booking booking) {
        return booking.getUser() != null ? booking.getUser().getEmail() : Constants.DEFAULT_NA;
    }

    private String getHotelName(Booking booking) {
        return (booking.getRoom() != null && booking.getRoom().getHotel() != null)
                ? booking.getRoom().getHotel().getName()
                : Constants.DEFAULT_NA;
    }

    private String getRoomName(Booking booking) {
        return booking.getRoom() != null ? booking.getRoom().getName() : Constants.DEFAULT_NA;
    }

    private String getRoomType(Booking booking) {
        return booking.getRoom() != null ? booking.getRoom().getType() : Constants.DEFAULT_NA;
    }

    private HotelResponse mapToHotelResponseSafely(Hotel hotel) {
        try {
            return hotelService.getHotelById(hotel.getId());
        } catch (Exception e) {
            return createFallbackHotelResponse(hotel);
        }
    }

    private HotelResponse createFallbackHotelResponse(Hotel hotel) {
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

        response.setOwnerName(hotel.getOwner() != null
                ? hotel.getOwner().getFirstName() + " " + hotel.getOwner().getLastName()
                : Constants.DEFAULT_NA);

        return response;
    }

    private RoomResponse mapToRoomResponseSafely(Room room) {
        try {
            return roomService.getRoomById(room.getId());
        } catch (Exception e) {
            log.warn("Error getting room details for ID {}: {}", room.getId(), e.getMessage());
            return createFallbackRoomResponse(room);
        }
    }

    private RoomResponse createFallbackRoomResponse(Room room) {
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

        response.setHotelName(room.getHotel() != null ? room.getHotel().getName() : Constants.DEFAULT_NA);
        return response;
    }

    private ReviewResponse mapToReviewResponseSafely(com.hotelreservation.model.Review review) {
        try {
            return reviewService.getReviewById(review.getId());
        } catch (Exception e) {
            return createFallbackReviewResponse(review);
        }
    }

    private ReviewResponse createFallbackReviewResponse(com.hotelreservation.model.Review review) {
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

        if (review.getBooking() != null && review.getBooking().getRoom() != null
                && review.getBooking().getRoom().getHotel() != null) {
            Hotel hotel = review.getBooking().getRoom().getHotel();
            response.setHotelName(hotel.getName());
            response.setHotel(HotelResponse.builder()
                    .id(hotel.getId())
                    .name(hotel.getName())
                    .build());
        }

        return response;
    }
}
