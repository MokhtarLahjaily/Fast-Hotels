package com.hotelreservation.repository;

import com.hotelreservation.model.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {

    @Query("SELECT a FROM Amenity a JOIN a.hotels h WHERE h.id = :hotelId")
    List<Amenity> findByHotelId(@Param("hotelId") Long hotelId);

    // Alternative native query approach
    @Query(value = "SELECT a.* FROM amenities a " +
            "JOIN hotel_amenities ha ON a.id = ha.amenity_id " +
            "WHERE ha.hotel_id = :hotelId", nativeQuery = true)
    List<Amenity> findByHotelIdNative(@Param("hotelId") Long hotelId);
}
