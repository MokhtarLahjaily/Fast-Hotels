package com.hotelreservation.repository;

import com.hotelreservation.model.Booking;
import com.hotelreservation.model.BookingStatus;
import com.hotelreservation.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Page<Booking> findByUser(User user, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.room.hotel.id = :hotelId")
    Page<Booking> findByHotelId(@Param("hotelId") Long hotelId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status = :status")
    Page<Booking> findByUserAndStatus(@Param("userId") Long userId, @Param("status") BookingStatus status, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE " +
            "b.room.id = :roomId AND " +
            "b.status IN ('CONFIRMED', 'PENDING') AND " +
            "((b.checkInDate <= :checkOut AND b.checkOutDate >= :checkIn))")
    List<Booking> findOverlappingBookings(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    // New method for finding recent bookings for a user
    List<Booking> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // Method to find bookings with a specific status and check-out date before a given date
    List<Booking> findByStatusAndCheckOutDateBefore(BookingStatus status, LocalDate date);

    // Method to find bookings with a specific status and check-in date before a given date
    List<Booking> findByStatusAndCheckInDateBefore(BookingStatus status, LocalDate date);

    // Method to find bookings with a specific status and check-in date before and check-out date after a given date
    List<Booking> findByStatusAndCheckInDateBeforeAndCheckOutDateAfter(
            BookingStatus status, LocalDate checkInThreshold, LocalDate checkOutThreshold);

    // Method to count bookings by status
    Long countByStatus(BookingStatus status);
}
