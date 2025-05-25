package com.hotelreservation.repository;

import com.hotelreservation.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // Using native query to handle potential bytea columns
    @Query(value = "SELECT u.* FROM users u WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(u.email::text) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.first_name::text) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.last_name::text) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:role IS NULL OR :role = '' OR u.role = :role) " +
            "ORDER BY u.id DESC",
            countQuery = "SELECT COUNT(*) FROM users u WHERE " +
                    "(:search IS NULL OR :search = '' OR " +
                    "LOWER(u.email::text) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(u.first_name::text) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(u.last_name::text) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
                    "(:role IS NULL OR :role = '' OR u.role = :role)",
            nativeQuery = true)
    Page<User> findUsersWithFilters(@Param("search") String search,
                                    @Param("role") String role,
                                    @Param("status") String status,
                                    Pageable pageable);
}
