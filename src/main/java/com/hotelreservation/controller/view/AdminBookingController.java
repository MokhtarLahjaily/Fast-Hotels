package com.hotelreservation.controller.view;

import com.hotelreservation.dto.response.BookingResponse;
import com.hotelreservation.model.BookingStatus;
import com.hotelreservation.service.BookingService;
import com.hotelreservation.service.BookingStatusUpdateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private static final Logger logger = LoggerFactory.getLogger(AdminBookingController.class);
    private final BookingService bookingService;
    private final BookingStatusUpdateService bookingStatusUpdateService;

    @GetMapping
    public String bookingsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateRange,
            Model model) {

        logger.info("Loading admin bookings page - page: {}, size: {}", page, size);

        try {
            // Get booking counts by status
            long pendingCount = bookingService.countByStatus(BookingStatus.PENDING);
            long confirmedCount = bookingService.countByStatus(BookingStatus.CONFIRMED);
            long completedCount = bookingService.countByStatus(BookingStatus.COMPLETED);
            long cancelledCount = bookingService.countByStatus(BookingStatus.CANCELLED);

            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("confirmedCount", confirmedCount);
            model.addAttribute("completedCount", completedCount);
            model.addAttribute("cancelledCount", cancelledCount);

            // Get paginated bookings
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<BookingResponse> bookingsPage = bookingService.getAllBookings(pageable);

            model.addAttribute("bookings", bookingsPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", bookingsPage.getTotalPages());
            model.addAttribute("totalElements", bookingsPage.getTotalElements());

            // Add search parameters back to model
            model.addAttribute("search", search);
            model.addAttribute("status", status);
            model.addAttribute("dateRange", dateRange);

            return "admin/bookings";
        } catch (Exception e) {
            logger.error("Error loading admin bookings page", e);
            model.addAttribute("errorMessage", "An error occurred while loading the bookings page: " + e.getMessage());
            return "admin/bookings";
        }
    }

    @PostMapping("/update-statuses")
    public String updateBookingStatuses(RedirectAttributes redirectAttributes) {
        logger.info("Manually updating booking statuses");

        try {
            int updatedCount = bookingStatusUpdateService.updateAllBookingStatuses();
            redirectAttributes.addFlashAttribute("success",
                    "Successfully updated " + updatedCount + " booking(s) status.");
        } catch (Exception e) {
            logger.error("Error updating booking statuses", e);
            redirectAttributes.addFlashAttribute("error",
                    "An error occurred while updating booking statuses: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/{id}/complete")
    public String completeBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        logger.info("Manually completing booking with ID: {}", id);

        try {
            BookingResponse booking = bookingService.completeBooking(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully marked booking #" + id + " as completed.");
        } catch (Exception e) {
            logger.error("Error completing booking", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An error occurred while completing the booking: " + e.getMessage());
        }

        return "redirect:/admin/bookings";
    }

    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        logger.info("Manually cancelling booking with ID: {}", id);

        try {
            BookingResponse booking = bookingService.cancelBooking(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully cancelled booking #" + id + ".");
        } catch (Exception e) {
            logger.error("Error cancelling booking", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An error occurred while cancelling the booking: " + e.getMessage());
        }

        return "redirect:/admin/bookings";
    }

    @PostMapping("/{id}/confirm")
    public String confirmBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        logger.info("Manually confirming booking with ID: {}", id);

        try {
            BookingResponse booking = bookingService.confirmBooking(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully confirmed booking #" + id + ".");
        } catch (Exception e) {
            logger.error("Error confirming booking", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An error occurred while confirming the booking: " + e.getMessage());
        }

        return "redirect:/admin/bookings";
    }

    @GetMapping("/{id}")
    public String viewBookingDetails(@PathVariable Long id, Model model) {
        logger.info("Viewing admin booking details for ID: {}", id);

        try {
            BookingResponse booking = bookingService.getBookingById(id);
            model.addAttribute("booking", booking);

            // Get booking counts by status for the summary
            long pendingCount = bookingService.countByStatus(BookingStatus.PENDING);
            long confirmedCount = bookingService.countByStatus(BookingStatus.CONFIRMED);
            long completedCount = bookingService.countByStatus(BookingStatus.COMPLETED);
            long cancelledCount = bookingService.countByStatus(BookingStatus.CANCELLED);

            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("confirmedCount", confirmedCount);
            model.addAttribute("completedCount", completedCount);
            model.addAttribute("cancelledCount", cancelledCount);

            return "admin/bookings";
        } catch (Exception e) {
            logger.error("Error viewing booking details", e);
            model.addAttribute("errorMessage", "An error occurred while loading the booking details: " + e.getMessage());
            return "admin/bookings";
        }
    }
}
