package com.hotelreservation.service;

import com.hotelreservation.dto.request.ReviewRequest;
import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.dto.response.ReviewResponse;
import com.hotelreservation.dto.response.UserResponse;
import com.hotelreservation.exception.BadRequestException;
import com.hotelreservation.exception.ResourceNotFoundException;
import com.hotelreservation.exception.UnauthorizedException;
import com.hotelreservation.model.Booking;
import com.hotelreservation.model.BookingStatus;
import com.hotelreservation.model.Hotel;
import com.hotelreservation.model.Review;
import com.hotelreservation.model.User;
import com.hotelreservation.repository.BookingRepository;
import com.hotelreservation.repository.HotelRepository;
import com.hotelreservation.repository.ReviewRepository;
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

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final HotelService hotelService;

    public Page<ReviewResponse> getReviewsByHotel(Long hotelId, Pageable pageable) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));

        return reviewRepository.findByHotel(hotel, pageable)
                .map(this::mapToReviewResponse);
    }

    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        // Check if the booking belongs to the current user
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to review this booking");
        }

        // Check if the booking is completed
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("You can only review completed bookings");
        }

        // Check if a review already exists for this booking
        if (booking.getReview() != null) {
            throw new BadRequestException("A review already exists for this booking");
        }

        Hotel hotel = booking.getRoom().getHotel();

        Review review = Review.builder()
                .booking(booking)
                .user(user)
                .hotel(hotel)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review savedReview = reviewRepository.save(review);
        return mapToReviewResponse(savedReview);
    }

    @Transactional
    public ReviewResponse updateReview(Long id, ReviewRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Check if the review belongs to the current user
        if (!review.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to update this review");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review updatedReview = reviewRepository.save(review);
        return mapToReviewResponse(updatedReview);
    }

    @Transactional
    public void deleteReview(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Check if the review belongs to the current user
        if (!review.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to delete this review");
        }

        reviewRepository.delete(review);
    }

    private ReviewResponse mapToReviewResponse(Review review) {
        HotelResponse hotelResponse = hotelService.getHotelById(review.getHotel().getId());

        return ReviewResponse.builder()
                .id(review.getId())
                .bookingId(review.getBooking().getId())
                .user(mapToUserResponse(review.getUser()))
                .hotel(hotelResponse)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
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

    // New method for the frontend
    public Page<ReviewResponse> getHotelReviews(Long hotelId, Pageable pageable) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));

        return reviewRepository.findByHotel(hotel, pageable)
                .map(this::mapToReviewResponse);
    }

    /**
     * Get reviews for a specific hotel with pagination
     *
     * @param id The hotel ID
     * @param pageable Pagination information
     * @return Page of ReviewResponse objects
     */
    public Page<ReviewResponse> getReviewsByHotelId(Long id, Pageable pageable) {
        logger.info("Getting reviews for hotel with ID: {}, page: {}, size: {}",
                id, pageable.getPageNumber(), pageable.getPageSize());

        try {
            // Check if hotel exists
            Hotel hotel = hotelRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + id));

            // Get reviews with pagination
            return reviewRepository.findByHotel(hotel, pageable)
                    .map(this::mapToReviewResponse);
        } catch (ResourceNotFoundException e) {
            logger.warn("Hotel not found with ID: {}", id);
            throw e;
        } catch (Exception e) {
            logger.error("Error getting reviews for hotel with ID: {}", id, e);
            throw new RuntimeException("Error retrieving reviews: " + e.getMessage());
        }
    }
}


