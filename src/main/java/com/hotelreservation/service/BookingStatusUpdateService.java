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
            // Update CONFIRMED bookings with past check-out dates to COMPLETED
            List<Booking> completedBookings = bookingRepository.findByStatusAndCheckOutDateBefore(
                    BookingStatus.CONFIRMED, today);

            log.info("Found {} CONFIRMED bookings with past check-out dates to mark as COMPLETED",
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
                booking.setStatus(BookingStatus.ACTIVE);
                bookingRepository.save(booking);
                totalUpdated++;
                log.debug("Updated booking {} from CONFIRMED to ACTIVE (check-in: {}, check-out: {})",
                        booking.getId(), booking.getCheckInDate(), booking.getCheckOutDate());
            }

            // Optional: Update PENDING bookings with past check-in dates to CANCELLED
            // (if guest didn't show up and booking wasn't confirmed)
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
