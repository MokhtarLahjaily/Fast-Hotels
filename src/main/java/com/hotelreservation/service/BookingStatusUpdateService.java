package com.hotelreservation.service;

import com.hotelreservation.model.Booking;
import com.hotelreservation.model.BookingStatus;
import com.hotelreservation.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingStatusUpdateService {

    private final BookingRepository bookingRepository;

    /**
     * Updates booking statuses daily at midnight.
     * - CONFIRMED bookings with check-out date before today become COMPLETED
     * - CONFIRMED bookings with check-in date before today and check-out date after today become ACTIVE
     */
    @Scheduled(cron = "0 0 0 * * ?") // Run at midnight every day
    @Transactional
    public void updateBookingStatuses() {
        log.info("Starting scheduled booking status update");
        updateAllBookingStatuses();
        log.info("Completed scheduled booking status update");
    }

    /**
     * Updates all booking statuses and returns the total count of updated bookings.
     * This method can be called manually from admin controllers or other services.
     *
     * @return the total number of bookings that were updated
     */
    @Transactional
    public int updateAllBookingStatuses() {
        log.info("Starting booking status update process");

        LocalDate today = LocalDate.now();
        int totalUpdated = 0;

        try {
            // Update CONFIRMED bookings with check-out dates today or in the past to COMPLETED
            List<Booking> completedBookings = bookingRepository.findByStatusAndCheckOutDateBefore(
                    BookingStatus.CONFIRMED, today.plusDays(1)); // Include today

            log.info("Found {} CONFIRMED bookings with check-out dates today or past to mark as COMPLETED",
                    completedBookings.size());

            for (Booking booking : completedBookings) {
                booking.setStatus(BookingStatus.COMPLETED);
                bookingRepository.save(booking);
                totalUpdated++;
                log.debug("Updated booking {} from CONFIRMED to COMPLETED (check-out: {})",
                        booking.getId(), booking.getCheckOutDate());
            }

            // Update CONFIRMED bookings that are currently active
            // (check-in date has passed, check-out date has not)
            List<Booking> activeBookings = bookingRepository.findByStatusAndCheckInDateBeforeAndCheckOutDateAfter(
                    BookingStatus.CONFIRMED, today.plusDays(1), today.minusDays(1));

            log.info("Found {} CONFIRMED bookings that are currently active", activeBookings.size());

            for (Booking booking : activeBookings) {
                // For admin convenience, also mark these as completed if check-out is today
                if (booking.getCheckOutDate().equals(today) || booking.getCheckOutDate().isBefore(today)) {
                    booking.setStatus(BookingStatus.COMPLETED);
                    totalUpdated++;
                    log.debug("Updated booking {} from CONFIRMED to COMPLETED (check-in: {}, check-out: {})",
                            booking.getId(), booking.getCheckInDate(), booking.getCheckOutDate());
                } else {
                    booking.setStatus(BookingStatus.ACTIVE);
                    totalUpdated++;
                    log.debug("Updated booking {} from CONFIRMED to ACTIVE (check-in: {}, check-out: {})",
                            booking.getId(), booking.getCheckInDate(), booking.getCheckOutDate());
                }
                bookingRepository.save(booking);
            }

            // Optional: Update PENDING bookings with past check-in dates to CANCELLED
            List<Booking> expiredPendingBookings = bookingRepository.findByStatusAndCheckInDateBefore(
                    BookingStatus.PENDING, today.minusDays(1)); // Grace period of 1 day

            log.info("Found {} PENDING bookings with past check-in dates to mark as CANCELLED",
                    expiredPendingBookings.size());

            for (Booking booking : expiredPendingBookings) {
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);
                totalUpdated++;
                log.debug("Updated booking {} from PENDING to CANCELLED (check-in: {})",
                        booking.getId(), booking.getCheckInDate());
            }

            log.info("Successfully updated {} bookings in total", totalUpdated);

        } catch (Exception e) {
            log.error("Error occurred while updating booking statuses: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update booking statuses", e);
        }

        return totalUpdated;
    }

    /**
     * Manually trigger booking status updates.
     * This is a convenience method that calls updateAllBookingStatuses.
     *
     * @return the number of bookings that were updated
     */
    @Transactional
    public int manualUpdateBookingStatuses() {
        log.info("Starting manual booking status update");
        int updatedCount = updateAllBookingStatuses();
        log.info("Completed manual booking status update, {} bookings updated", updatedCount);
        return updatedCount;
    }

    /**
     * Get statistics about bookings that need status updates without actually updating them.
     * Useful for admin dashboards or reporting.
     *
     * @return a summary of bookings that would be updated
     */
    @Transactional(readOnly = true)
    public BookingStatusUpdateSummary getUpdateSummary() {
        LocalDate today = LocalDate.now();

        int completedCount = bookingRepository.findByStatusAndCheckOutDateBefore(
                BookingStatus.CONFIRMED, today).size();

        int activeCount = bookingRepository.findByStatusAndCheckInDateBeforeAndCheckOutDateAfter(
                BookingStatus.CONFIRMED, today.plusDays(1), today.minusDays(1)).size();

        int expiredPendingCount = bookingRepository.findByStatusAndCheckInDateBefore(
                BookingStatus.PENDING, today.minusDays(1)).size();

        return new BookingStatusUpdateSummary(completedCount, activeCount, expiredPendingCount);
    }

    /**
     * Mark all eligible bookings as completed.
     * This is more aggressive than the date-based update and will complete
     * any CONFIRMED booking that has passed its check-out date.
     *
     * @return the number of bookings marked as completed
     */
    @Transactional
    public int markAllEligibleBookingsAsCompleted() {
        log.info("Starting bulk completion of eligible bookings");

        LocalDate today = LocalDate.now();
        int completedCount = 0;

        try {
            // Find all CONFIRMED bookings where check-out date has passed (including today)
            List<Booking> eligibleBookings = bookingRepository.findByStatusAndCheckOutDateBefore(
                    BookingStatus.CONFIRMED, today.plusDays(1));

            log.info("Found {} CONFIRMED bookings eligible for completion", eligibleBookings.size());

            for (Booking booking : eligibleBookings) {
                booking.setStatus(BookingStatus.COMPLETED);
                bookingRepository.save(booking);
                completedCount++;
                log.debug("Marked booking {} as COMPLETED (check-out: {})",
                        booking.getId(), booking.getCheckOutDate());
            }

            log.info("Successfully marked {} bookings as COMPLETED", completedCount);

        } catch (Exception e) {
            log.error("Error occurred while marking bookings as completed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to mark bookings as completed", e);
        }

        return completedCount;
    }

    /**
     * Inner class to hold summary information about booking status updates
     */
    public static class BookingStatusUpdateSummary {
        private final int toCompleted;
        private final int toActive;
        private final int toCancelled;

        public BookingStatusUpdateSummary(int toCompleted, int toActive, int toCancelled) {
            this.toCompleted = toCompleted;
            this.toActive = toActive;
            this.toCancelled = toCancelled;
        }

        public int getToCompleted() { return toCompleted; }
        public int getToActive() { return toActive; }
        public int getToCancelled() { return toCancelled; }
        public int getTotal() { return toCompleted + toActive + toCancelled; }

        @Override
        public String toString() {
            return String.format("BookingStatusUpdateSummary{toCompleted=%d, toActive=%d, toCancelled=%d, total=%d}",
                    toCompleted, toActive, toCancelled, getTotal());
        }
    }
}
