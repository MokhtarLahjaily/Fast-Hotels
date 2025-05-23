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

        // Update the booking to reference the review
        booking.setReview(savedReview);
        bookingRepository.save(booking);

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
        logger.info("Starting deletion process for review ID: {}", id);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        logger.info("User attempting deletion: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        logger.info("Found review: ID={}, User={}, Hotel={}",
                review.getId(),
                review.getUser().getEmail(),
                review.getHotel().getName());

        // Check if the review belongs to the current user
        if (!review.getUser().getId().equals(user.getId())) {
            logger.warn("Unauthorized deletion attempt: review belongs to user {}, but {} is trying to delete",
                    review.getUser().getEmail(), email);
            throw new UnauthorizedException("You are not authorized to delete this review");
        }

        try {
            // First, remove the review reference from the booking
            Booking booking = review.getBooking();
            if (booking != null) {
                logger.info("Removing review reference from booking ID: {}", booking.getId());
                booking.setReview(null);
                bookingRepository.save(booking);
                bookingRepository.flush(); // Force immediate save
                logger.info("Successfully removed review reference from booking");
            }

            // Now delete the review using custom query
            logger.info("Deleting review from database using custom query");
            reviewRepository.deleteReviewById(id);
            logger.info("Successfully deleted review ID: {}", id);

        } catch (Exception e) {
            logger.error("Error during review deletion: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete review: " + e.getMessage(), e);
        }
    }

    private ReviewResponse mapToReviewResponse(Review review) {
        try {
            HotelResponse hotelResponse = hotelService.getHotelById(review.getHotel().getId());

            ReviewResponse response = ReviewResponse.builder()
                    .id(review.getId())
                    .bookingId(review.getBooking().getId())
                    .user(mapToUserResponse(review.getUser()))
                    .hotel(hotelResponse)
                    .rating(review.getRating())
                    .comment(review.getComment())
                    .createdAt(review.getCreatedAt())
                    .updatedAt(review.getUpdatedAt())
                    .build();

            // Set additional fields
            if (review.getUser() != null) {
                response.setUserEmail(review.getUser().getEmail());
                String userName = "";
                if (review.getUser().getFirstName() != null) {
                    userName += review.getUser().getFirstName();
                }
                if (review.getUser().getLastName() != null) {
                    if (!userName.isEmpty()) userName += " ";
                    userName += review.getUser().getLastName();
                }
                response.setUserName(userName.isEmpty() ? "Guest" : userName);
            }

            if (review.getHotel() != null) {
                response.setHotelName(review.getHotel().getName());
            }

            return response;
        } catch (Exception e) {
            logger.error("Error mapping review to response: {}", e.getMessage(), e);
            // Return a basic response if detailed mapping fails
            return ReviewResponse.builder()
                    .id(review.getId())
                    .bookingId(review.getBooking() != null ? review.getBooking().getId() : null)
                    .rating(review.getRating())
                    .comment(review.getComment())
                    .createdAt(review.getCreatedAt())
                    .updatedAt(review.getUpdatedAt())
                    .userEmail(review.getUser() != null ? review.getUser().getEmail() : "Unknown")
                    .userName("Guest")
                    .hotelName(review.getHotel() != null ? review.getHotel().getName() : "Unknown Hotel")
                    .build();
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

    /**
     * Get a specific review by ID
     *
     * @param id The review ID
     * @return ReviewResponse object
     */
    public ReviewResponse getReviewById(Long id) {
        logger.info("Getting review with ID: {}", id);

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + id));

        return mapToReviewResponse(review);
    }

    /**
     * Get reviews for the current authenticated user
     *
     * @param pageable Pagination information
     * @return Page of ReviewResponse objects
     */
    public Page<ReviewResponse> getCurrentUserReviews(Pageable pageable) {
        logger.info("Getting reviews for current user, page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return reviewRepository.findByUser(user, pageable)
                .map(this::mapToReviewResponse);
    }

    /**
     * Check if a booking has been reviewed
     *
     * @param bookingId The booking ID
     * @return true if the booking has been reviewed, false otherwise
     */
    public boolean hasBookingBeenReviewed(Long bookingId) {
        logger.info("Checking if booking has been reviewed: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        return booking.getReview() != null;
    }
}
