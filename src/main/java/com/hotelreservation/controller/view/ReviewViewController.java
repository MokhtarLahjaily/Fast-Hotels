package com.hotelreservation.controller.view;

import com.hotelreservation.dto.request.ReviewRequest;
import com.hotelreservation.dto.response.BookingResponse;
import com.hotelreservation.dto.response.ReviewResponse;
import com.hotelreservation.exception.BadRequestException;
import com.hotelreservation.exception.ResourceNotFoundException;
import com.hotelreservation.exception.UnauthorizedException;
import com.hotelreservation.service.BookingService;
import com.hotelreservation.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReviewViewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewViewController.class);

    private final ReviewService reviewService;
    private final BookingService bookingService;

    @GetMapping
    public String viewMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        logger.info("Viewing my reviews page: {}, size: {}", page, size);

        try {
            Page<ReviewResponse> reviews = reviewService.getCurrentUserReviews(PageRequest.of(page, size));

            model.addAttribute("reviews", reviews.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", reviews.getTotalPages());
            model.addAttribute("totalItems", reviews.getTotalElements());

            return "review/list";
        } catch (Exception e) {
            logger.error("Error viewing my reviews", e);
            model.addAttribute("errorMessage", "An error occurred while loading your reviews. Please try again.");
            return "review/list";
        }
    }

    @GetMapping("/write/{bookingId}")
    public String showReviewForm(@PathVariable Long bookingId, Model model) {
        logger.info("Showing review form for booking: {}", bookingId);

        try {
            // Get booking details
            BookingResponse booking = bookingService.getBookingById(bookingId);

            // Check if booking is eligible for review
            if (!bookingService.isEligibleForReview(bookingId)) {
                logger.warn("Booking {} is not eligible for review", bookingId);
                return "redirect:/bookings/" + bookingId + "?error=This+booking+is+not+eligible+for+review";
            }

            // Check if booking already has a review
            if (reviewService.hasBookingBeenReviewed(bookingId)) {
                logger.warn("Booking {} already has a review", bookingId);
                return "redirect:/bookings/" + bookingId + "?error=You+have+already+reviewed+this+booking";
            }

            // Create empty review request
            ReviewRequest reviewRequest = new ReviewRequest();
            reviewRequest.setBookingId(bookingId);

            model.addAttribute("booking", booking);
            model.addAttribute("reviewRequest", reviewRequest);

            return "review/form";
        } catch (ResourceNotFoundException e) {
            logger.warn("Booking not found: {}", bookingId);
            return "redirect:/bookings?error=Booking+not+found";
        } catch (Exception e) {
            logger.error("Error showing review form for booking: {}", bookingId, e);
            return "redirect:/bookings?error=An+error+occurred";
        }
    }

    @PostMapping("/submit")
    public String submitReview(
            @Valid @ModelAttribute("reviewRequest") ReviewRequest reviewRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        logger.info("Submitting review for booking: {}", reviewRequest.getBookingId());

        if (bindingResult.hasErrors()) {
            logger.warn("Review validation failed: {}", bindingResult.getAllErrors());

            try {
                // Get booking details to redisplay the form
                BookingResponse booking = bookingService.getBookingById(reviewRequest.getBookingId());
                model.addAttribute("booking", booking);
                return "review/form";
            } catch (Exception e) {
                logger.error("Error getting booking details after validation failure", e);
                return "redirect:/bookings";
            }
        }

        try {
            // Check if booking is eligible for review
            if (!bookingService.isEligibleForReview(reviewRequest.getBookingId())) {
                logger.warn("Booking {} is not eligible for review", reviewRequest.getBookingId());
                redirectAttributes.addFlashAttribute("errorMessage", "This booking is not eligible for review.");
                return "redirect:/bookings/" + reviewRequest.getBookingId();
            }

            // Check if booking already has a review
            if (reviewService.hasBookingBeenReviewed(reviewRequest.getBookingId())) {
                logger.warn("Booking {} already has a review", reviewRequest.getBookingId());
                redirectAttributes.addFlashAttribute("errorMessage", "You have already reviewed this booking.");
                return "redirect:/bookings/" + reviewRequest.getBookingId();
            }

            ReviewResponse review = reviewService.createReview(reviewRequest);
            logger.info("Review submitted successfully for booking: {}", reviewRequest.getBookingId());

            redirectAttributes.addFlashAttribute("successMessage", "Your review has been submitted successfully! Thank you for your feedback.");
            return "redirect:/bookings/" + reviewRequest.getBookingId();
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized review submission: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to review this booking.");
            return "redirect:/bookings/" + reviewRequest.getBookingId();
        } catch (BadRequestException e) {
            logger.warn("Bad request for review submission: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/bookings/" + reviewRequest.getBookingId();
        } catch (Exception e) {
            logger.error("Error submitting review", e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while submitting your review. Please try again.");
            return "redirect:/bookings/" + reviewRequest.getBookingId();
        }
    }

    @GetMapping("/edit/{reviewId}")
    public String showEditForm(@PathVariable Long reviewId, Model model) {
        logger.info("Showing edit form for review: {}", reviewId);

        try {
            ReviewResponse review = reviewService.getReviewById(reviewId);

            // Create review request from existing review
            ReviewRequest reviewRequest = new ReviewRequest();
            reviewRequest.setBookingId(review.getBookingId());
            reviewRequest.setRating(review.getRating());
            reviewRequest.setComment(review.getComment());

            // Get booking details
            BookingResponse booking = bookingService.getBookingById(review.getBookingId());

            model.addAttribute("review", review);
            model.addAttribute("booking", booking);
            model.addAttribute("reviewRequest", reviewRequest);
            model.addAttribute("isEdit", true);

            return "review/form";
        } catch (ResourceNotFoundException e) {
            logger.warn("Review not found: {}", reviewId);
            return "redirect:/reviews?error=Review+not+found";
        } catch (Exception e) {
            logger.error("Error showing edit form for review: {}", reviewId, e);
            return "redirect:/reviews?error=An+error+occurred";
        }
    }

    @PostMapping("/update/{reviewId}")
    public String updateReview(
            @PathVariable Long reviewId,
            @Valid @ModelAttribute("reviewRequest") ReviewRequest reviewRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        logger.info("Updating review: {}", reviewId);

        if (bindingResult.hasErrors()) {
            logger.warn("Review update validation failed: {}", bindingResult.getAllErrors());

            try {
                // Get booking details to redisplay the form
                BookingResponse booking = bookingService.getBookingById(reviewRequest.getBookingId());
                ReviewResponse review = reviewService.getReviewById(reviewId);

                model.addAttribute("booking", booking);
                model.addAttribute("review", review);
                model.addAttribute("isEdit", true);
                return "review/form";
            } catch (Exception e) {
                logger.error("Error getting details after validation failure", e);
                return "redirect:/reviews";
            }
        }

        try {
            ReviewResponse review = reviewService.updateReview(reviewId, reviewRequest);
            logger.info("Review updated successfully: {}", reviewId);

            redirectAttributes.addFlashAttribute("successMessage", "Your review has been updated successfully!");
            return "redirect:/reviews";
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized review update: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to update this review.");
            return "redirect:/reviews";
        } catch (ResourceNotFoundException e) {
            logger.warn("Review not found for update: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Review not found.");
            return "redirect:/reviews";
        } catch (Exception e) {
            logger.error("Error updating review", e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while updating your review. Please try again.");
            return "redirect:/reviews";
        }
    }

    @PostMapping("/delete/{reviewId}")
    public String deleteReview(
            @PathVariable Long reviewId,
            RedirectAttributes redirectAttributes) {

        logger.info("Attempting to delete review: {}", reviewId);

        try {
            // First, verify the review exists and get its details for logging
            ReviewResponse review = reviewService.getReviewById(reviewId);
            logger.info("Found review to delete: ID={}, BookingID={}", reviewId, review.getBookingId());

            // Delete the review
            reviewService.deleteReview(reviewId);
            logger.info("Review deleted successfully: {}", reviewId);

            redirectAttributes.addFlashAttribute("successMessage", "Your review has been deleted successfully!");

        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized review deletion attempt for review {}: {}", reviewId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to delete this review.");
        } catch (ResourceNotFoundException e) {
            logger.warn("Review not found for deletion: {}", reviewId);
            redirectAttributes.addFlashAttribute("errorMessage", "Review not found or has already been deleted.");
        } catch (Exception e) {
            logger.error("Unexpected error deleting review {}: {}", reviewId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while deleting your review. Please try again.");
        }

        return "redirect:/reviews";
    }

    @GetMapping("/delete/{reviewId}")
    public String deleteReviewGet(
            @PathVariable Long reviewId,
            RedirectAttributes redirectAttributes) {

        logger.warn("GET request received for review deletion: {}. Redirecting to reviews list.", reviewId);
        redirectAttributes.addFlashAttribute("errorMessage", "Invalid request. Please use the delete button to remove reviews.");
        return "redirect:/reviews";
    }
}
