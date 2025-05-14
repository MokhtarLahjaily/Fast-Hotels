package com.hotelreservation.service;

import com.hotelreservation.dto.response.AmenityResponse;
import com.hotelreservation.model.Amenity;
import com.hotelreservation.repository.AmenityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AmenityService {

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

    private AmenityResponse mapToAmenityResponse(Amenity amenity) {
        return AmenityResponse.builder()
                .id(amenity.getId())
                .name(amenity.getName())
                .icon(amenity.getIcon())
                .build();
    }
}
