package com.hotelreservation.repository;

import com.hotelreservation.model.Hotel;
import com.hotelreservation.model.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Page<Room> findByHotel(Hotel hotel, Pageable pageable);

    @Query("SELECT r FROM Room r WHERE r.hotel.id = :hotelId AND " +
            "r.capacity >= :guestCount AND " +
            "r.id NOT IN (" +
            "    SELECT ri.room.id FROM RoomInventory ri " +
            "    WHERE ri.date BETWEEN :checkIn AND :checkOut AND ri.availableCount = 0" +
            ")")
    List<Room> findAvailableRooms(
            @Param("hotelId") Long hotelId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("guestCount") Integer guestCount
    );

    @Query("SELECT r FROM Room r WHERE r.hotel.id = :hotelId AND " +
            "r.capacity >= :guestCount AND " +
            "r.pricePerNight BETWEEN :minPrice AND :maxPrice")
    Page<Room> findByPriceRange(
            @Param("hotelId") Long hotelId,
            @Param("guestCount") Integer guestCount,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    List<Room> findByHotelId(Long id);
}
