package com.hotelreservation.service;

import com.hotelreservation.dto.request.RoomRequest;
import com.hotelreservation.dto.response.AmenityResponse;
import com.hotelreservation.dto.response.ImageResponse;
import com.hotelreservation.dto.response.RoomResponse;
import com.hotelreservation.exception.ResourceNotFoundException;
import com.hotelreservation.exception.UnauthorizedException;
import com.hotelreservation.model.Amenity;
import com.hotelreservation.model.Hotel;
import com.hotelreservation.model.Image;
import com.hotelreservation.model.Room;
import com.hotelreservation.model.User;
import com.hotelreservation.repository.AmenityRepository;
import com.hotelreservation.repository.HotelRepository;
import com.hotelreservation.repository.ImageRepository;
import com.hotelreservation.repository.RoomRepository;
import com.hotelreservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final AmenityRepository amenityRepository;
    private final ImageRepository imageRepository;

    public Page<RoomResponse> getRoomsByHotel(Long hotelId, Pageable pageable) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));

        return roomRepository.findByHotel(hotel, pageable)
                .map(this::mapToRoomResponse);
    }

    public List<RoomResponse> getAvailableRooms(Long hotelId, LocalDate checkIn, LocalDate checkOut,
            Integer guestCount) {
        return roomRepository.findAvailableRooms(hotelId, checkIn, checkOut, guestCount)
                .stream()
                .map(this::mapToRoomResponse)
                .toList();
    }

    public Page<RoomResponse> getRoomsByPriceRange(Long hotelId, Integer guestCount, BigDecimal minPrice,
            BigDecimal maxPrice, Pageable pageable) {
        if (minPrice == null) {
            minPrice = BigDecimal.ZERO;
        }

        if (maxPrice == null) {
            maxPrice = new BigDecimal("999999.99"); // A very high value
        }

        return roomRepository.findByPriceRange(hotelId, guestCount, minPrice, maxPrice, pageable)
                .map(this::mapToRoomResponse);
    }

    public RoomResponse getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        return mapToRoomResponse(room);
    }

    @Transactional
    public RoomResponse createRoom(Long hotelId, RoomRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));

        if (!hotel.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to add rooms to this hotel");
        }

        Set<Amenity> amenities = new HashSet<>();
        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            amenities = request.getAmenityIds().stream()
                    .map(id -> amenityRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Amenity not found")))
                    .collect(Collectors.toSet());
        }

        Room room = Room.builder()
                .hotel(hotel)
                .name(request.getName())
                .description(request.getDescription())
                .capacity(request.getCapacity())
                .pricePerNight(request.getPricePerNight())
                .roomCount(request.getRoomCount())
                .amenities(amenities)
                .build();

        Room savedRoom = roomRepository.save(room);
        return mapToRoomResponse(savedRoom);
    }

    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (!room.getHotel().getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to update this room");
        }

        Set<Amenity> amenities = new HashSet<>();
        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            amenities = request.getAmenityIds().stream()
                    .map(amenityId -> amenityRepository.findById(amenityId)
                            .orElseThrow(() -> new ResourceNotFoundException("Amenity not found")))
                    .collect(Collectors.toSet());
        }

        room.setName(request.getName());
        room.setDescription(request.getDescription());
        room.setCapacity(request.getCapacity());
        room.setPricePerNight(request.getPricePerNight());
        room.setRoomCount(request.getRoomCount());
        room.setAmenities(amenities);

        Room updatedRoom = roomRepository.save(room);
        return mapToRoomResponse(updatedRoom);
    }

    @Transactional
    public void deleteRoom(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (!room.getHotel().getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to delete this room");
        }

        roomRepository.delete(room);
    }

    private RoomResponse mapToRoomResponse(Room room) {
        List<Image> images = imageRepository.findByEntityTypeAndEntityId("ROOM", room.getId());

        return RoomResponse.builder()
                .id(room.getId())
                .hotelId(room.getHotel().getId())
                .hotelName(room.getHotel().getName())
                .name(room.getName())
                .description(room.getDescription())
                .capacity(room.getCapacity())
                .pricePerNight(room.getPricePerNight())
                .roomCount(room.getRoomCount())
                .type(room.getType())
                .amenities(room.getAmenities().stream()
                        .map(this::mapToAmenityResponse)
                        .collect(Collectors.toList()))
                .images(images.stream()
                        .map(this::mapToImageResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private AmenityResponse mapToAmenityResponse(Amenity amenity) {
        return AmenityResponse.builder()
                .id(amenity.getId())
                .name(amenity.getName())
                .icon(amenity.getIcon())
                .build();
    }

    private ImageResponse mapToImageResponse(Image image) {
        return ImageResponse.builder()
                .id(image.getId())
                .url(image.getUrl())
                .isPrimary(image.getIsPrimary())
                .build();
    }

    /**
     * Get available rooms for a specific hotel, date range, and guest count
     *
     * @param hotelId  The hotel ID
     * @param checkIn  Check-in date
     * @param checkOut Check-out date
     * @param guests   Number of guests
     * @return List of available RoomResponse objects
     */
    public List<RoomResponse> getAvailableRoomsByHotelId(Long hotelId, LocalDate checkIn, LocalDate checkOut,
            Integer guests) {
        logger.info("Getting available rooms for hotel ID: {}, checkIn: {}, checkOut: {}, guests: {}",
                hotelId, checkIn, checkOut, guests);

        try {
            // Validate input
            if (checkIn == null || checkOut == null || guests == null) {
                logger.warn("Invalid input parameters for getAvailableRoomsByHotelId");
                return Collections.emptyList();
            }

            if (checkIn.isAfter(checkOut) || checkIn.isBefore(LocalDate.now())) {
                logger.warn("Invalid date range: checkIn={}, checkOut={}", checkIn, checkOut);
                return Collections.emptyList();
            }

            // Check if hotel exists
            Hotel hotel = hotelRepository.findById(hotelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + hotelId));

            // Get available rooms
            return roomRepository.findAvailableRooms(hotelId, checkIn, checkOut, guests)
                    .stream()
                    .map(this::mapToRoomResponse)
                    .toList();
        } catch (ResourceNotFoundException e) {
            logger.warn("Hotel not found with ID: {}", hotelId);
            throw e;
        } catch (Exception e) {
            logger.error("Error getting available rooms for hotel ID: {}", hotelId, e);
            throw new RuntimeException("Error retrieving available rooms: " + e.getMessage());
        }
    }
}
