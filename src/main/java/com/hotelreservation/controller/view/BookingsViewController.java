package com.hotelreservation.controller.view;

import com.hotelreservation.dto.response.BookingResponse;
import com.hotelreservation.service.BookingService;
import com.hotelreservation.service.ReviewService;
import com.hotelreservation.util.Constants;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingsViewController {

    private static final Logger logger = LoggerFactory.getLogger(BookingsViewController.class);
    private final BookingService bookingService;
    private final ReviewService reviewService;

    @GetMapping
    public String listBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        logger.info("Loading bookings page: page={}, size={}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<BookingResponse> bookings = bookingService.getCurrentUserBookings(pageable);

            model.addAttribute("bookings", bookings.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", bookings.getTotalPages());
            model.addAttribute("totalItems", bookings.getTotalElements());

            return "booking/list";
        } catch (Exception e) {
            logger.error("Error loading bookings page", e);
            model.addAttribute(Constants.ATTR_ERROR_MSG,
                    "An error occurred while loading your bookings: " + e.getMessage());
            return "booking/list";
        }
    }

    @GetMapping("/{id}")
    public String viewBooking(@PathVariable Long id, Model model) {
        logger.info("Loading booking details for id: {}", id);

        try {
            BookingResponse booking = bookingService.getBookingById(id);
            model.addAttribute("booking", booking);

            // Check if the booking is eligible for review
            boolean isEligibleForReview = bookingService.isEligibleForReview(id);
            model.addAttribute("isEligibleForReview", isEligibleForReview);

            // Check if the booking already has a review
            boolean hasReview = reviewService.hasBookingBeenReviewed(id);
            model.addAttribute("hasReview", hasReview);

            return "booking/detail";
        } catch (Exception e) {
            logger.error("Error loading booking details", e);
            model.addAttribute(Constants.ATTR_ERROR_MSG,
                    "An error occurred while loading the booking details: " + e.getMessage());
            return "booking/detail";
        }
    }

    @GetMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        logger.info("Cancelling booking with id: {}", id);

        try {
            BookingResponse booking = bookingService.cancelBooking(id);
            redirectAttributes.addFlashAttribute(Constants.ATTR_SUCCESS_MSG,
                    "Your booking has been successfully cancelled. Booking reference: #" + booking.getId());
            return Constants.REDIRECT_BOOKINGS;
        } catch (Exception e) {
            logger.error("Error cancelling booking", e);
            redirectAttributes.addFlashAttribute(Constants.ATTR_ERROR_MSG,
                    "An error occurred while cancelling your booking: " + e.getMessage());
            return "redirect:/bookings/" + id;
        }
    }
}
