package com.hotelreservation.repository;

import com.hotelreservation.model.Room;
import com.hotelreservation.model.RoomInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomInventoryRepository extends JpaRepository<RoomInventory, Long> {
    Optional<RoomInventory> findByRoomAndDate(Room room, LocalDate date);

    @Query("SELECT ri FROM RoomInventory ri WHERE ri.room.id = :roomId AND ri.date BETWEEN :startDate AND :endDate")
    List<RoomInventory> findByRoomAndDateRange(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT ri FROM RoomInventory ri WHERE ri.room.hotel.id = :hotelId AND ri.date BETWEEN :startDate AND :endDate AND ri.availableCount > 0")
    List<RoomInventory> findAvailableInventoryByHotelAndDateRange(
            @Param("hotelId") Long hotelId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
