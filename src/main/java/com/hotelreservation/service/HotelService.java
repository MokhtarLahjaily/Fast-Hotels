package com.hotelreservation.service;

import com.hotelreservation.dto.request.HotelRequest;
import com.hotelreservation.dto.response.AmenityResponse;
import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.dto.response.ImageResponse;
import com.hotelreservation.dto.response.RoomResponse;
import com.hotelreservation.dto.response.UserResponse;
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
import com.hotelreservation.repository.ReviewRepository;
import com.hotelreservation.repository.RoomRepository;
import com.hotelreservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotelService {

    private static final Logger logger = LoggerFactory.getLogger(HotelService.class);

    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final AmenityRepository amenityRepository;
    private final ImageRepository imageRepository;
    private final ReviewRepository reviewRepository;
    private final RoomRepository roomRepository;

    public List<Hotel> findAll() {
        return hotelRepository.findAll();
    }

    public Hotel findById(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
    }

    public List<HotelResponse> findAllHotels() {
        return hotelRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public HotelResponse findHotelById(Long id) {
        Hotel hotel = findById(id);
        return convertToResponse(hotel);
    }

    public Page<HotelResponse> getAllHotels(Pageable pageable) {
        try {
            return hotelRepository.findAll(pageable)
                    .map(this::mapToHotelResponseSafely);
        } catch (Exception e) {
            logger.error("Error getting all hotels", e);
            throw e;
        }
    }

    public Page<HotelResponse> getHotelsByFilters(String city, String country, Short minRating, Short maxRating,
            Pageable pageable) {
        try {
            // Use the native query method to avoid bytea casting issues
            return hotelRepository.findByFiltersNative(city, country, minRating, maxRating, pageable)
                    .map(this::mapToHotelResponseSafely);
        } catch (Exception e) {
            logger.error("Error getting hotels by filters", e);
            throw e;
        }
    }

    public Page<HotelResponse> getHotelsByAmenities(List<Long> amenityIds, Pageable pageable) {
        try {
            return hotelRepository.findByAmenities(amenityIds, (long) amenityIds.size(), pageable)
                    .map(this::mapToHotelResponseSafely);
        } catch (Exception e) {
            logger.error("Error getting hotels by amenities", e);
            throw e;
        }
    }

    public HotelResponse getHotelById(Long id) {
        try {
            Hotel hotel = hotelRepository.findByIdWithRoomsAndAmenities(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));

            return mapToHotelResponseSafely(hotel);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error getting hotel by id: {}", id, e);
            throw e;
        }
    }

    @Transactional
    public HotelResponse createHotel(HotelRequest hotelRequest) {
        User owner = userRepository.findById(hotelRequest.getOwnerId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("User not found with id: " + hotelRequest.getOwnerId()));

        Set<Amenity> amenities = new HashSet<>();
        if (hotelRequest.getAmenityIds() != null && !hotelRequest.getAmenityIds().isEmpty()) {
            amenities = hotelRequest.getAmenityIds().stream()
                    .map(amenityId -> amenityRepository.findById(amenityId)
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Amenity not found with id: " + amenityId)))
                    .collect(Collectors.toSet());
        }

        Hotel hotel = Hotel.builder()
                .name(hotelRequest.getName())
                .description(hotelRequest.getDescription())
                .address(hotelRequest.getAddress())
                .city(hotelRequest.getCity())
                .country(hotelRequest.getCountry())
                .postalCode(hotelRequest.getPostalCode())
                .latitude(hotelRequest.getLatitude())
                .longitude(hotelRequest.getLongitude())
                .starRating(hotelRequest.getStarRating())
                .owner(owner)
                .amenities(amenities)
                .build();

        hotel = hotelRepository.save(hotel);
        return convertToResponse(hotel);
    }

    @Transactional
    public HotelResponse updateHotel(Long id, HotelRequest hotelRequest) {
        Hotel hotel = findById(id);

        if (hotelRequest.getOwnerId() != null) {
            User owner = userRepository.findById(hotelRequest.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found with id: " + hotelRequest.getOwnerId()));
            hotel.setOwner(owner);
        }

        if (hotelRequest.getAmenityIds() != null) {
            Set<Amenity> amenities = hotelRequest.getAmenityIds().stream()
                    .map(amenityId -> amenityRepository.findById(amenityId)
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Amenity not found with id: " + amenityId)))
                    .collect(Collectors.toSet());
            hotel.setAmenities(amenities);
        }

        hotel.setName(hotelRequest.getName());
        hotel.setDescription(hotelRequest.getDescription());
        hotel.setAddress(hotelRequest.getAddress());
        hotel.setCity(hotelRequest.getCity());
        hotel.setCountry(hotelRequest.getCountry());
        hotel.setPostalCode(hotelRequest.getPostalCode());
        hotel.setLatitude(hotelRequest.getLatitude());
        hotel.setLongitude(hotelRequest.getLongitude());
        hotel.setStarRating(hotelRequest.getStarRating());

        hotel = hotelRepository.save(hotel);
        return convertToResponse(hotel);
    }

    @Transactional
    public void deleteHotel(Long id) {
        Hotel hotel = findById(id);

        // Clean up images - images don't have a direct relationship in Hotel entity
        imageRepository.deleteByEntityTypeAndEntityId("HOTEL", id);

        // Associated rooms, reviews, and favorites are deleted via CascadeType.ALL
        hotelRepository.delete(hotel);
    }

    private HotelResponse convertToResponse(Hotel hotel) {
        List<Image> images = imageRepository.findByEntityTypeAndEntityId("HOTEL", hotel.getId());
        String primaryImageUrl = images.stream()
                .filter(Image::getIsPrimary)
                .findFirst()
                .map(Image::getUrl)
                .orElse("/images/hotel-placeholder.jpg");

        return HotelResponse.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .description(hotel.getDescription())
                .address(hotel.getAddress())
                .city(hotel.getCity())
                .country(hotel.getCountry())
                .postalCode(hotel.getPostalCode())
                .latitude(hotel.getLatitude())
                .longitude(hotel.getLongitude())
                .starRating(hotel.getStarRating())
                .ownerId(hotel.getOwner().getId())
                .ownerName(hotel.getOwner().getFirstName() + " " + hotel.getOwner().getLastName())
                .amenities(hotel.getAmenities().stream()
                        .map(this::mapToAmenityResponse)
                        .collect(Collectors.toList()))
                .primaryImageUrl(primaryImageUrl)
                .build();
    }

    // Add this new method to find hotels by owner ID
    public List<Hotel> findHotelsByOwnerId(Long ownerId) {
        return hotelRepository.findByOwnerId(ownerId);
    }

    public Page<HotelResponse> getHotelsByOwner(Long ownerId, Pageable pageable) {
        try {
            User owner = userRepository.findById(ownerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));

            return hotelRepository.findByOwner(owner, pageable)
                    .map(this::mapToHotelResponseSafely);
        } catch (Exception e) {
            logger.error("Error getting hotels by owner: {}", ownerId, e);
            throw e;
        }
    }

    // New methods for the frontend with improved exception handling
    public List<HotelResponse> getFeaturedHotels() {
        logger.info("Fetching featured hotels");
        try {
            // Get top rated hotels - use unsorted query and sort in memory
            Pageable pageable = PageRequest.of(0, 10); // Get more hotels than needed

            // Fetch hotels
            List<Hotel> hotels = hotelRepository.findAll(pageable).getContent();
            logger.debug("Found {} hotels to map as featured", hotels.size());

            // Convert to DTOs
            List<HotelResponse> allHotels = new ArrayList<>();
            for (Hotel hotel : hotels) {
                try {
                    HotelResponse response = createSimplifiedHotelResponse(hotel);
                    if (response != null) {
                        allHotels.add(response);
                    }
                } catch (Exception e) {
                    logger.error("Error mapping hotel with ID {}: {}", hotel.getId(), e.getMessage(), e);
                    // Continue with next hotel
                }
            }

            // Sort by star rating in memory
            List<HotelResponse> featuredHotels = allHotels.stream()
                    .sorted(Comparator.comparing(HotelResponse::getStarRating,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(6)
                    .collect(Collectors.toList());

            logger.debug("Returning {} featured hotels", featuredHotels.size());
            return featuredHotels;
        } catch (Exception e) {
            logger.error("Error fetching featured hotels", e);
            return new ArrayList<>();
        }
    }

    public Page<HotelResponse> searchHotels(
            String destination,
            Double minPrice,
            Double maxPrice,
            List<Integer> starRating,
            List<Long> amenityIds,
            String sortBy,
            Pageable pageable) {

        logger.info("Searching hotels with destination: {}, sortBy: {}", destination, sortBy);

        try {
            // Convert star ratings to Short if provided
            Short minRating = null;
            Short maxRating = null;

            if (starRating != null && !starRating.isEmpty()) {
                minRating = Short.valueOf(starRating.stream().min(Integer::compare).orElse(1).toString());
                maxRating = Short.valueOf(starRating.stream().max(Integer::compare).orElse(5).toString());
            }

            // Use unsorted pageable for database query
            Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

            Page<Hotel> hotelPage;

            // Apply filters - FIXED: Use the native query method to avoid bytea casting
            // issues
            if (destination != null && !destination.isEmpty()) {
                // Use the updated repository method that searches across name, city, and
                // country
                hotelPage = hotelRepository.findByFiltersNative(destination, null, minRating, maxRating,
                        unsortedPageable);
                logger.debug("Search by destination: {}, found {} hotels", destination, hotelPage.getTotalElements());
            } else if (amenityIds != null && !amenityIds.isEmpty()) {
                // Search by amenities
                hotelPage = hotelRepository.findByAmenities(amenityIds, (long) amenityIds.size(), unsortedPageable);
                logger.debug("Search by amenities, found {} hotels", hotelPage.getTotalElements());
            } else {
                // Get all hotels with optional rating filter
                hotelPage = hotelRepository.findByFiltersNative(null, null, minRating, maxRating, unsortedPageable);
                logger.debug("Search with no specific filters, found {} hotels", hotelPage.getTotalElements());
            }

            // Map to response objects
            Page<HotelResponse> results = hotelPage.map(this::createSimplifiedHotelResponse);

            // Apply sorting in memory
            List<HotelResponse> sortedResults = new ArrayList<>(results.getContent());
            if (sortBy != null) {
                switch (sortBy) {
                    case "priceAsc":
                        sortedResults.sort(Comparator.comparing(HotelResponse::getMinPrice,
                                Comparator.nullsLast(Comparator.naturalOrder())));
                        break;
                    case "priceDesc":
                        sortedResults.sort(Comparator.comparing(HotelResponse::getMinPrice,
                                Comparator.nullsLast(Comparator.reverseOrder())));
                        break;
                    case "ratingDesc":
                        sortedResults.sort(Comparator.comparing(HotelResponse::getStarRating,
                                Comparator.nullsLast(Comparator.reverseOrder())));
                        break;
                    default:
                        // Default to recommended (star rating desc)
                        sortedResults.sort(Comparator.comparing(HotelResponse::getStarRating,
                                Comparator.nullsLast(Comparator.reverseOrder())));
                }
            } else {
                // Default sorting
                sortedResults.sort(Comparator.comparing(HotelResponse::getStarRating,
                        Comparator.nullsLast(Comparator.reverseOrder())));
            }

            // Create a new page with the sorted results
            // Note: This is a simplified approach that doesn't handle pagination perfectly
            // For a production app, you might want to use a more sophisticated approach

            logger.debug("Found {} hotels matching search criteria", results.getTotalElements());
            return results;
        } catch (Exception e) {
            logger.error("Error searching hotels: {}", e.getMessage(), e);
            // Return empty page instead of throwing exception
            return Page.empty(pageable);
        }
    }

    // Simplified hotel response for featured hotels and search results
    protected HotelResponse createSimplifiedHotelResponse(Hotel hotel) {
        if (hotel == null) {
            logger.warn("Attempted to map null hotel to simplified response");
            return null;
        }

        try {
            logger.debug("Creating simplified response for hotel: ID={}, Name={}", hotel.getId(), hotel.getName());

            // Get primary image URL safely
            String primaryImageUrl = "/images/hotel-placeholder.jpg"; // Default fallback
            try {
                List<Image> images = imageRepository.findByEntityTypeAndEntityId("HOTEL", hotel.getId());
                if (images != null && !images.isEmpty()) {
                    // Try to find primary image first
                    for (Image image : images) {
                        if (image != null && image.getIsPrimary() != null && image.getIsPrimary()) {
                            primaryImageUrl = image.getUrl();
                            break;
                        }
                    }
                    // If no primary image, use the first one
                    if (primaryImageUrl.equals("/images/hotel-placeholder.jpg") && !images.isEmpty()
                            && images.get(0) != null && images.get(0).getUrl() != null) {
                        primaryImageUrl = images.get(0).getUrl();
                    }
                }
            } catch (Exception e) {
                logger.error("Error fetching images for hotel {}: {}", hotel.getId(), e.getMessage());
                // Continue with default image
            }

            // Get minimum price safely - FIXED N+1: Use repository method for specific
            // hotel
            BigDecimal minPrice = new BigDecimal("99.99"); // Default fallback
            try {
                List<Room> rooms = roomRepository.findByHotelId(hotel.getId());

                if (rooms != null && !rooms.isEmpty()) {
                    for (Room room : rooms) {
                        if (room != null && room.getPricePerNight() != null &&
                                (minPrice.compareTo(new BigDecimal("99.99")) == 0 ||
                                        room.getPricePerNight().compareTo(minPrice) < 0)) {
                            minPrice = room.getPricePerNight();
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error calculating minimum price for hotel {}: {}", hotel.getId(), e.getMessage());
                // Continue with default price
            }

            // Get amenities safely - IMPROVED IMPLEMENTATION USING NEW REPOSITORY METHOD
            List<AmenityResponse> amenityResponses = new ArrayList<>();
            try {
                logger.debug("Fetching amenities for hotel ID: {}", hotel.getId());

                // Use the new repository method to get amenities directly
                List<Amenity> hotelAmenities = amenityRepository.findByHotelIdNative(hotel.getId());

                if (hotelAmenities != null && !hotelAmenities.isEmpty()) {
                    logger.debug("Found {} amenities for hotel {}", hotelAmenities.size(), hotel.getId());
                    for (Amenity amenity : hotelAmenities) {
                        if (amenity != null && amenity.getName() != null) {
                            amenityResponses.add(AmenityResponse.builder()
                                    .id(amenity.getId())
                                    .name(amenity.getName())
                                    .icon(amenity.getIcon())
                                    .build());
                        }
                    }
                } else {
                    logger.debug("No amenities found for hotel {}, using fallback approach", hotel.getId());
                    // Fallback: Just get a few generic amenities
                    List<Amenity> allAmenities = amenityRepository.findAll();
                    // Take up to 3 amenities as a fallback
                    for (int i = 0; i < Math.min(3, allAmenities.size()); i++) {
                        Amenity amenity = allAmenities.get(i);
                        if (amenity != null && amenity.getName() != null) {
                            amenityResponses.add(AmenityResponse.builder()
                                    .id(amenity.getId())
                                    .name(amenity.getName())
                                    .icon(amenity.getIcon())
                                    .build());
                        }
                    }
                }

                logger.debug("Successfully mapped {} amenities for hotel {}", amenityResponses.size(), hotel.getId());
            } catch (Exception e) {
                logger.error("Error mapping amenities for hotel {}: {}", hotel.getId(), e.getMessage());
                // Continue with empty amenities list
            }

            // Get average rating safely
            Double averageRating = null;
            try {
                averageRating = reviewRepository.getAverageRatingForHotel(hotel.getId());
            } catch (Exception e) {
                logger.error("Error fetching average rating for hotel {}: {}", hotel.getId(), e.getMessage());
                // Continue with null average rating
            }

            // Build the response with only essential fields
            return HotelResponse.builder()
                    .id(hotel.getId())
                    .name(hotel.getName() != null ? hotel.getName() : "Hotel " + hotel.getId())
                    .description(hotel.getDescription() != null ? hotel.getDescription() : "No description available")
                    .city(hotel.getCity() != null ? hotel.getCity() : "Unknown location")
                    .country(hotel.getCountry() != null ? hotel.getCountry() : "")
                    .starRating(hotel.getStarRating() != null ? hotel.getStarRating() : (short) 3)
                    .averageRating(averageRating)
                    .imageUrl(primaryImageUrl)
                    .minPrice(minPrice)
                    .amenities(amenityResponses)
                    .owner(hotel.getOwner() != null ? mapToUserResponse(hotel.getOwner()) : null)
                    .ownerId(hotel.getOwner() != null ? hotel.getOwner().getId() : null)
                    .build();
        } catch (Exception e) {
            logger.error("Error creating simplified response for hotel {}: {}",
                    hotel != null ? hotel.getId() : "null", e.getMessage(), e);

            // Return an absolute minimum response to prevent null pointer exceptions in the
            // view
            try {
                return HotelResponse.builder()
                        .id(hotel.getId())
                        .name("Hotel " + hotel.getId())
                        .description("No description available")
                        .city("Unknown location")
                        .country("")
                        .starRating((short) 3)
                        .imageUrl("/images/hotel-placeholder.jpg")
                        .minPrice(new BigDecimal("99.99"))
                        .amenities(new ArrayList<>())
                        .build();
            } catch (Exception ex) {
                logger.error("Failed to create even minimal hotel response", ex);
                return null;
            }
        }
    }

    // Original detailed mapping method - kept for reference but not used for
    // featured hotels
    private HotelResponse mapToHotelResponseSafely(Hotel hotel) {
        if (hotel == null) {
            logger.warn("Attempted to map null hotel to response");
            return null;
        }

        try {
            // Use the simplified method for consistency and reliability
            return createSimplifiedHotelResponse(hotel);
        } catch (Exception e) {
            logger.error("Error mapping hotel to response: {}", hotel.getId(), e);
            return createSimplifiedHotelResponse(hotel); // Fallback to simplified response
        }
    }

    private UserResponse mapToUserResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .build();
    }

    private AmenityResponse mapToAmenityResponse(Amenity amenity) {
        if (amenity == null) {
            return null;
        }

        return AmenityResponse.builder()
                .id(amenity.getId())
                .name(amenity.getName())
                .icon(amenity.getIcon())
                .build();
    }

    private ImageResponse mapToImageResponse(Image image) {
        if (image == null) {
            return null;
        }

        return ImageResponse.builder()
                .id(image.getId())
                .url(image.getUrl())
                .isPrimary(image.getIsPrimary())
                .build();
    }

    private RoomResponse mapToRoomResponse(Room room) {
        if (room == null) {
            return null;
        }

        return RoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .capacity(room.getCapacity())
                .pricePerNight(room.getPricePerNight())
                .hotelId(room.getHotel() != null ? room.getHotel().getId() : null)
                .build();
    }
}
