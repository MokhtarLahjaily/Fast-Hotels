package com.hotelreservation.controller.view;

import com.hotelreservation.model.BookingStatus;
import com.hotelreservation.dto.response.BookingResponse;
import com.hotelreservation.service.AdminService;
import com.hotelreservation.service.BookingService;
import com.hotelreservation.service.BookingStatusUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@RequestMapping("/admin/bookings")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminBookingController {

    private final AdminService adminService;
    private final BookingService bookingService;
    private final BookingStatusUpdateService bookingStatusUpdateService;

    @GetMapping("")
    public String bookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateRange,
            Model model) {
        log.info("Accessing admin bookings page - page: {}, size: {}, search: {}", page, size, search);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            Page<BookingResponse> bookingsPage = adminService.getAllBookings(pageable, search, status, dateRange);

            // Get booking counts by status
            long pendingCount = bookingService.countByStatus(BookingStatus.PENDING);
            long confirmedCount = bookingService.countByStatus(BookingStatus.CONFIRMED);
            long completedCount = bookingService.countByStatus(BookingStatus.COMPLETED);
            long cancelledCount = bookingService.countByStatus(BookingStatus.CANCELLED);

            model.addAttribute("bookings", bookingsPage);
            model.addAttribute("search", search);
            model.addAttribute("status", status);
            model.addAttribute("dateRange", dateRange);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", bookingsPage.getTotalPages());
            model.addAttribute("totalElements", bookingsPage.getTotalElements());

            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("confirmedCount", confirmedCount);
            model.addAttribute("completedCount", completedCount);
            model.addAttribute("cancelledCount", cancelledCount);

            log.info("Loaded {} bookings for admin page", bookingsPage.getTotalElements());
            return "admin/bookings";
        } catch (Exception e) {
            log.error("Error loading admin bookings page: {}", e.getMessage(), e);
            model.addAttribute("error", "Error loading bookings: " + e.getMessage());
            model.addAttribute("bookings", Page.empty());
            return "admin/bookings";
        }
    }

    @PostMapping("/update-statuses")
    public String updateBookingStatuses(RedirectAttributes redirectAttributes) {
        log.info("Manually updating booking statuses");

        try {
            int updatedCount = bookingStatusUpdateService.updateAllBookingStatuses();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully updated " + updatedCount + " booking(s) status.");
        } catch (Exception e) {
            log.error("Error updating booking statuses", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An error occurred while updating booking statuses: " + e.getMessage());
        }

        return "redirect:/admin/bookings";
    }

    @PostMapping("/complete-eligible")
    public String completeEligibleBookings(RedirectAttributes redirectAttributes) {
        log.info("Manually completing all eligible bookings");

        try {
            int completedCount = bookingStatusUpdateService.markAllEligibleBookingsAsCompleted();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully marked " + completedCount + " booking(s) as completed.");
        } catch (Exception e) {
            log.error("Error completing eligible bookings", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An error occurred while completing bookings: " + e.getMessage());
        }

        return "redirect:/admin/bookings";
    }

    @PostMapping("/{id}/complete")
    public String completeBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("Manually completing booking with ID: {}", id);

        try {
            BookingResponse booking = bookingService.getBookingByIdForAdmin(id);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Booking not found with ID: " + id);
                return "redirect:/admin/bookings";
            }

            // Update booking status to COMPLETED
            bookingService.updateBookingStatus(id, BookingStatus.COMPLETED);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully marked booking #" + id + " as completed.");
        } catch (Exception e) {
            log.error("Error completing booking with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An error occurred while completing the booking: " + e.getMessage());
        }

        return "redirect:/admin/bookings";
    }

    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("Manually cancelling booking with ID: {}", id);

        try {
            BookingResponse booking = bookingService.getBookingByIdForAdmin(id);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Booking not found with ID: " + id);
                return "redirect:/admin/bookings";
            }

            // Update booking status to CANCELLED
            bookingService.updateBookingStatus(id, BookingStatus.CANCELLED);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully cancelled booking #" + id + ".");
        } catch (Exception e) {
            log.error("Error cancelling booking with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An error occurred while cancelling the booking: " + e.getMessage());
        }

        return "redirect:/admin/bookings";
    }

    @PostMapping("/{id}/confirm")
    public String confirmBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("Manually confirming booking with ID: {}", id);

        try {
            BookingResponse booking = bookingService.getBookingByIdForAdmin(id);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Booking not found with ID: " + id);
                return "redirect:/admin/bookings";
            }

            // Use the confirmBooking method from BookingService
            bookingService.confirmBooking(id);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully confirmed booking #" + id + ".");
        } catch (Exception e) {
            log.error("Error confirming booking with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An error occurred while confirming the booking: " + e.getMessage());
        }

        return "redirect:/admin/bookings";
    }

    @GetMapping("/{id}")
    public String viewBookingDetails(@PathVariable Long id, Model model) {
        log.info("Viewing admin booking details for ID: {}", id);

        try {
            BookingResponse booking = bookingService.getBookingByIdForAdmin(id);
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
            log.error("Error viewing booking details", e);
            model.addAttribute("errorMessage", "An error occurred while loading the booking details: " + e.getMessage());
            return "admin/bookings";
        }
    }
}
