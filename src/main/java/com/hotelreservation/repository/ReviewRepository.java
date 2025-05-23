package com.hotelreservation.repository;

import com.hotelreservation.model.Hotel;
import com.hotelreservation.model.Review;
import com.hotelreservation.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByHotel(Hotel hotel, Pageable pageable);
    Page<Review> findByUser(User user, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.hotel.id = :hotelId")
    Double getAverageRatingForHotel(@Param("hotelId") Long hotelId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Review r WHERE r.id = :reviewId")
    void deleteReviewById(@Param("reviewId") Long reviewId);

    @Query("SELECT r FROM Review r WHERE r.id = :reviewId AND r.user.id = :userId")
    Review findByIdAndUserId(@Param("reviewId") Long reviewId, @Param("userId") Long userId);
}
