package com.hotelreservation.service;

import com.hotelreservation.dto.response.AmenityResponse;
import com.hotelreservation.model.Amenity;
import com.hotelreservation.repository.AmenityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AmenityService {

    private static final Logger logger = LoggerFactory.getLogger(AmenityService.class);
    private final AmenityRepository amenityRepository;

    @Autowired
    public AmenityService(AmenityRepository amenityRepository) {
        this.amenityRepository = amenityRepository;
    }

    public List<AmenityResponse> getAllAmenities() {
        return amenityRepository.findAll().stream()
                .map(this::mapToAmenityResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of common amenity names for use in AI context
     * @return List of amenity names as strings
     */
    public List<String> getCommonAmenities() {
        try {
            // Get the most common amenities (limit to 10)
            return amenityRepository.findAll().stream()
                    .map(Amenity::getName)
                    .limit(10)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching common amenities: {}", e.getMessage(), e);
            // Return a default list if there's an error
            return new ArrayList<>(List.of(
                    "WiFi", "Breakfast", "Swimming Pool", "Parking",
                    "Air Conditioning", "Restaurant", "Fitness Center",
                    "Room Service", "Bar", "Spa"
            ));
        }
    }

    private AmenityResponse mapToAmenityResponse(Amenity amenity) {
        return AmenityResponse.builder()
                .id(amenity.getId())
                .name(amenity.getName())
                .icon(amenity.getIcon())
                .build();
    }
}
