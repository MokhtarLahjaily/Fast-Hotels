package com.hotelreservation.repository;

import com.hotelreservation.model.Favorite;
import com.hotelreservation.model.Hotel;
import com.hotelreservation.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserAndHotel(User user, Hotel hotel);

    boolean existsByUserIdAndHotelId(Long userId, Long hotelId);

    Optional<Favorite> findByUserAndHotel(User user, Hotel hotel);

    List<Favorite> findByUserId(Long userId);

    List<Favorite> findByUser(User user);

    Page<Favorite> findByUser(User user, Pageable pageable);

    long countByUser(User user);

    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.user = :user AND f.hotel = :hotel")
    void deleteByUserAndHotel(@Param("user") User user, @Param("hotel") Hotel hotel);

    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.user.id = :userId AND f.hotel.id = :hotelId")
    void deleteByUserIdAndHotelId(@Param("userId") Long userId, @Param("hotelId") Long hotelId);

    @Query("SELECT f.hotel FROM Favorite f WHERE f.user = :user")
    List<Hotel> findHotelsByUser(@Param("user") User user);

    @Query("SELECT f.hotel FROM Favorite f WHERE f.user = :user")
    Page<Hotel> findHotelsByUser(@Param("user") User user, Pageable pageable);
}
