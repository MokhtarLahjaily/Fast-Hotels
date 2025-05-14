package com.hotelreservation.controller.api;

import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.service.AiRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final AiRecommendationService aiRecommendationService;

    @GetMapping("/hotels")
    public ResponseEntity<List<HotelResponse>> getRecommendedHotels(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String preferences) {
        return ResponseEntity.ok(aiRecommendationService.getRecommendedHotels(location, preferences));
    }
}
