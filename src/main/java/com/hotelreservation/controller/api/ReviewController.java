package com.hotelreservation.controller.api;

import com.hotelreservation.dto.request.ReviewRequest;
import com.hotelreservation.dto.response.ReviewResponse;
import com.hotelreservation.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/api/hotels/{hotelId}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByHotel(
            @PathVariable Long hotelId,
            Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByHotel(hotelId, pageable));
    }

    @PostMapping("/api/bookings/{bookingId}/reviews")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long bookingId,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.createReview(request));
    }

    @PutMapping("/api/reviews/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.updateReview(id, request));
    }

    @DeleteMapping("/api/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
