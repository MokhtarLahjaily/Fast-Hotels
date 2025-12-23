package com.hotelreservation.repository;

import com.hotelreservation.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<Image> findByEntityTypeAndEntityIdAndIsPrimaryTrue(String entityType, Long entityId);

    void deleteByEntityTypeAndEntityId(String entityType, Long entityId);
}
