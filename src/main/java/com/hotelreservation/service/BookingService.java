package com.hotelreservation.service;

import com.hotelreservation.dto.request.BookingRequest;
import com.hotelreservation.dto.response.BookingResponse;
import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.dto.response.RoomResponse;
import com.hotelreservation.dto.response.UserResponse;
import com.hotelreservation.exception.BadRequestException;
import com.hotelreservation.exception.ResourceNotFoundException;
import com.hotelreservation.exception.UnauthorizedException;
import com.hotelreservation.model.Booking;
import com.hotelreservation.model.BookingStatus;
import com.hotelreservation.model.Hotel;
import com.hotelreservation.model.Room;
import com.hotelreservation.model.RoomInventory;
import com.hotelreservation.model.User;
import com.hotelreservation.repository.BookingRepository;
import com.hotelreservation.repository.HotelRepository;
import com.hotelreservation.repository.RoomInventoryRepository;
import com.hotelreservation.repository.RoomRepository;
import com.hotelreservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomInventoryRepository roomInventoryRepository;
    private final RoomService roomService;
    private final HotelService hotelService;

    /**
     * Get recent bookings for the currently authenticated user
     * @param limit The maximum number of bookings to return
     * @return List of recent bookings
     */
    public List<BookingResponse> getRecentBookingsForCurrentUser(int limit) {
        log.debug("Fetching {} recent bookings for current user", limit);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Pageable pageable = PageRequest.of(0, limit);
        List<Booking> recentBookings = bookingRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        log.debug("Found {} recent bookings for user {}", recentBookings.size(), email);

        return recentBookings.stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    public Page<BookingResponse> getCurrentUserBookings(Pageable pageable) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return bookingRepository.findByUser(user, pageable)
                .map(this::mapToBookingResponse);
    }

    public BookingResponse getBookingById(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        // Check if the booking belongs to the current user or if the user is the hotel owner
        if (!booking.getUser().getId().equals(user.getId()) &&
                !booking.getRoom().getHotel().getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to view this booking");
        }

        return mapToBookingResponse(booking);
    }

    public Page<BookingResponse> getBookingsByHotel(Long hotelId, Pageable pageable) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));

        if (!hotel.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to view bookings for this hotel");
        }

        return bookingRepository.findByHotelId(hotelId, pageable)
                .map(this::mapToBookingResponse);
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        // Validate dates
        if (request.getCheckInDate().isAfter(request.getCheckOutDate())) {
            throw new BadRequestException("Check-in date must be before check-out date");
        }

        if (request.getCheckInDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Check-in date must be in the future");
        }

        // Check if the room has enough capacity
        if (room.getCapacity() < request.getGuestCount()) {
            throw new BadRequestException("Room capacity is not sufficient for the number of guests");
        }

        // Check if the room is available for the requested dates
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(
                room.getId(), request.getCheckInDate(), request.getCheckOutDate());

        int bookedRooms = overlappingBookings.size();
        if (bookedRooms >= room.getRoomCount()) {
            throw new BadRequestException("Room is not available for the selected dates");
        }

        // Calculate total price
        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal totalPrice = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        // Create booking
        Booking booking = Booking.builder()
                .user(user)
                .room(room)
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .guestCount(request.getGuestCount())
                .totalPrice(totalPrice)
                .specialRequests(request.getSpecialRequests())
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // Update room inventory
        updateRoomInventory(room, request.getCheckInDate(), request.getCheckOutDate());

        return mapToBookingResponse(savedBooking);
    }

    @Transactional
    public BookingResponse cancelBooking(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to cancel this booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed booking");
        }

        // Check cancellation policy (e.g., can't cancel if check-in is less than 24 hours away)
        if (booking.getCheckInDate().isBefore(LocalDate.now().plusDays(1))) {
            throw new BadRequestException("Booking cannot be cancelled within 24 hours of check-in");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking updatedBooking = bookingRepository.save(booking);

        // Update room inventory
        restoreRoomInventory(booking.getRoom(), booking.getCheckInDate(), booking.getCheckOutDate());

        return mapToBookingResponse(updatedBooking);
    }

    private void updateRoomInventory(Room room, LocalDate checkIn, LocalDate checkOut) {
        LocalDate currentDate = checkIn;
        while (!currentDate.isAfter(checkOut.minusDays(1))) {
            RoomInventory inventory = roomInventoryRepository.findByRoomAndDate(room, currentDate)
                    .orElse(RoomInventory.builder()
                            .room(room)
                            .date(currentDate)
                            .availableCount(room.getRoomCount())
                            .build());

            inventory.setAvailableCount(inventory.getAvailableCount() - 1);
            roomInventoryRepository.save(inventory);

            currentDate = currentDate.plusDays(1);
        }
    }

    private void restoreRoomInventory(Room room, LocalDate checkIn, LocalDate checkOut) {
        LocalDate currentDate = checkIn;
        while (!currentDate.isAfter(checkOut.minusDays(1))) {
            RoomInventory inventory = roomInventoryRepository.findByRoomAndDate(room, currentDate)
                    .orElse(null);

            if (inventory != null) {
                inventory.setAvailableCount(inventory.getAvailableCount() + 1);
                roomInventoryRepository.save(inventory);
            }

            currentDate = currentDate.plusDays(1);
        }
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        try {
            RoomResponse roomResponse = roomService.getRoomById(booking.getRoom().getId());
            HotelResponse hotelResponse = hotelService.getHotelById(booking.getRoom().getHotel().getId());

            // Create the booking response with all the necessary data
            BookingResponse response = BookingResponse.builder()
                    .id(booking.getId())
                    .user(mapToUserResponse(booking.getUser()))
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
                    .build();

            // Set the convenience properties for direct access in templates
            if (hotelResponse != null) {
                response.setHotelName(hotelResponse.getName());
            } else {
                response.setHotelName("N/A");
            }

            if (roomResponse != null) {
                // Use the name field as type if type is null
                response.setRoomType(roomResponse.getType() != null ? roomResponse.getType() : roomResponse.getName());
            } else {
                response.setRoomType("N/A");
            }

            return response;
        } catch (Exception e) {
            log.error("Error mapping booking to response: {}", e.getMessage(), e);

            // Create a minimal response with the essential data
            BookingResponse response = BookingResponse.builder()
                    .id(booking.getId())
                    .user(mapToUserResponse(booking.getUser()))
                    .checkInDate(booking.getCheckInDate())
                    .checkOutDate(booking.getCheckOutDate())
                    .guestCount(booking.getGuestCount())
                    .totalPrice(booking.getTotalPrice())
                    .specialRequests(booking.getSpecialRequests())
                    .status(booking.getStatus().name())
                    .createdAt(booking.getCreatedAt())
                    .updatedAt(booking.getUpdatedAt())
                    .hotelName("Error loading hotel")
                    .roomType("Error loading room")
                    .build();

            return response;
        }
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }
}
