package com.hotelreservation.repository;

import com.hotelreservation.model.Hotel;
import com.hotelreservation.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {
        Page<Hotel> findByOwner(User owner, Pageable pageable);

        // JPQL query with casting (may not work with bytea fields)
        // PERFORMANCE WARNING: Leading wildcard LIKE conditions ('%value%') prevent
        // B-Tree index usage and cause full table scans.
        // For production, consider using PostgreSQL pg_trgm extension with GIN indexes
        // or full-text search (ElasticSearch/Solr).
        @Query("SELECT h FROM Hotel h WHERE " +
                        "(:city IS NULL OR CAST(h.city AS string) LIKE CONCAT('%', CAST(:city AS string), '%') OR " +
                        "CAST(h.country AS string) LIKE CONCAT('%', CAST(:city AS string), '%') OR " +
                        "CAST(h.name AS string) LIKE CONCAT('%', CAST(:city AS string), '%')) AND " +
                        "(:country IS NULL OR CAST(h.country AS string) LIKE CONCAT('%', CAST(:country AS string), '%')) AND "
                        +
                        "(:minRating IS NULL OR h.starRating >= :minRating) AND " +
                        "(:maxRating IS NULL OR h.starRating <= :maxRating)")
        Page<Hotel> findByFilters(
                        @Param("city") String city,
                        @Param("country") String country,
                        @Param("minRating") Short minRating,
                        @Param("maxRating") Short maxRating,
                        Pageable pageable);

        @Query("SELECT h FROM Hotel h JOIN h.amenities a WHERE a.id IN :amenityIds GROUP BY h HAVING COUNT(a.id) = :amenityCount")
        Page<Hotel> findByAmenities(@Param("amenityIds") List<Long> amenityIds,
                        @Param("amenityCount") Long amenityCount, Pageable pageable);

        // Fixed native query with correct column name in ORDER BY clause
        @Query(value = "SELECT h.* FROM hotels h WHERE " +
                        "(:city IS NULL OR LOWER(h.city::text) LIKE LOWER(CONCAT('%', :city, '%')) OR " +
                        "LOWER(h.country::text) LIKE LOWER(CONCAT('%', :city, '%')) OR " +
                        "LOWER(h.name::text) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
                        "(:country IS NULL OR LOWER(h.country::text) LIKE LOWER(CONCAT('%', :country, '%'))) AND " +
                        "(:minRating IS NULL OR h.star_rating >= :minRating) AND " +
                        "(:maxRating IS NULL OR h.star_rating <= :maxRating)", countQuery = "SELECT COUNT(*) FROM hotels h WHERE "
                                        +
                                        "(:city IS NULL OR LOWER(h.city::text) LIKE LOWER(CONCAT('%', :city, '%')) OR "
                                        +
                                        "LOWER(h.country::text) LIKE LOWER(CONCAT('%', :city, '%')) OR " +
                                        "LOWER(h.name::text) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
                                        "(:country IS NULL OR LOWER(h.country::text) LIKE LOWER(CONCAT('%', :country, '%'))) AND "
                                        +
                                        "(:minRating IS NULL OR h.star_rating >= :minRating) AND " +
                                        "(:maxRating IS NULL OR h.star_rating <= :maxRating)", nativeQuery = true)
        Page<Hotel> findByFiltersNative(
                        @Param("city") String city,
                        @Param("country") String country,
                        @Param("minRating") Short minRating,
                        @Param("maxRating") Short maxRating,
                        Pageable pageable);

        @Query("SELECT DISTINCT h FROM Hotel h LEFT JOIN FETCH h.rooms LEFT JOIN FETCH h.amenities WHERE h.id = :id")
        java.util.Optional<Hotel> findByIdWithRoomsAndAmenities(@Param("id") Long id);

        List<Hotel> findByOwnerId(Long ownerId);

        @Query(value = "SELECT h.* FROM hotels h WHERE " +
                        "(:search IS NULL OR :search = '' OR " +
                        "LOWER(h.name::text) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(h.description::text) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(h.city::text) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(h.country::text) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
                        "(:city IS NULL OR :city = '' OR LOWER(h.city::text) LIKE LOWER(CONCAT('%', :city, '%')))", countQuery = "SELECT COUNT(*) FROM hotels h WHERE "
                                        +
                                        "(:search IS NULL OR :search = '' OR " +
                                        "LOWER(h.name::text) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                                        "LOWER(h.description::text) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                                        "LOWER(h.city::text) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                                        "LOWER(h.country::text) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
                                        "(:city IS NULL OR :city = '' OR LOWER(h.city::text) LIKE LOWER(CONCAT('%', :city, '%')))", nativeQuery = true)
        Page<Hotel> findHotelsWithFilters(@Param("search") String search,
                        @Param("city") String city,
                        @Param("status") String status,
                        Pageable pageable);
}
