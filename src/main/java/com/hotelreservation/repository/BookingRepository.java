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
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByUser(User user, Pageable pageable);

    Long countByStatus(BookingStatus status);

    // Find bookings by status and check-out date before a certain date
    List<Booking> findByStatusAndCheckOutDateBefore(BookingStatus status, LocalDate date);

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

    // Find bookings by status and check-in date before a certain date
    List<Booking> findByStatusAndCheckInDateBefore(BookingStatus status, LocalDate date);

    // Find bookings by status with check-in before one date and check-out after another date
    List<Booking> findByStatusAndCheckInDateBeforeAndCheckOutDateAfter(
            BookingStatus status, LocalDate checkInBefore, LocalDate checkOutAfter);

    // Admin filtering methods
    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    Page<Booking> findByCreatedAtAfter(LocalDateTime startDate, Pageable pageable);

    Page<Booking> findByUserEmailContainingIgnoreCase(String email, Pageable pageable);

    Page<Booking> findByUserEmailContainingIgnoreCaseAndStatus(String email, BookingStatus status, Pageable pageable);

    Page<Booking> findByUserEmailContainingIgnoreCaseAndCreatedAtAfter(String email, LocalDateTime startDate, Pageable pageable);

    Page<Booking> findByUserEmailContainingIgnoreCaseAndStatusAndCreatedAtAfter(String email, BookingStatus status, LocalDateTime startDate, Pageable pageable);

    Page<Booking> findByStatusAndCreatedAtAfter(BookingStatus status, LocalDateTime startDate, Pageable pageable);

    // Search by booking ID or user email
    @Query("SELECT b FROM Booking b WHERE " +
            "CAST(b.id AS string) LIKE %:search% OR " +
            "LOWER(b.user.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Booking> findByIdOrUserEmailContaining(@Param("search") String search, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE " +
            "(CAST(b.id AS string) LIKE %:search% OR " +
            "LOWER(b.user.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "b.status = :status")
    Page<Booking> findByIdOrUserEmailContainingAndStatus(@Param("search") String search, @Param("status") BookingStatus status, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE " +
            "(CAST(b.id AS string) LIKE %:search% OR " +
            "LOWER(b.user.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "b.createdAt >= :startDate")
    Page<Booking> findByIdOrUserEmailContainingAndCreatedAtAfter(@Param("search") String search, @Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE " +
            "(CAST(b.id AS string) LIKE %:search% OR " +
            "LOWER(b.user.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "b.status = :status AND " +
            "b.createdAt >= :startDate")
    Page<Booking> findByIdOrUserEmailContainingAndStatusAndCreatedAtAfter(@Param("search") String search, @Param("status") BookingStatus status, @Param("startDate") LocalDateTime startDate, Pageable pageable);

    List<Booking> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
