package com.hotelreservation.util;

import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.dto.response.ImageResponse;
import com.hotelreservation.model.Hotel;
import com.hotelreservation.model.Image;
import com.hotelreservation.repository.ImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ImageUtils {

    private static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);

    private static final String DEFAULT_HOTEL_IMAGE = "/images/hotel-placeholder.jpg";
    private static final String DEFAULT_ROOM_IMAGE = "/images/room-placeholder.jpg";

    private final ImageRepository imageRepository;

    public ImageUtils(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public String getHotelPrimaryImageUrl(Hotel hotel) {
        if (hotel == null) {
            return DEFAULT_HOTEL_IMAGE;
        }

        try {
            List<Image> images = imageRepository.findByEntityTypeAndEntityId("HOTEL", hotel.getId());
            if (images == null || images.isEmpty()) {
                return DEFAULT_HOTEL_IMAGE;
            }

            Optional<Image> primaryImage = images.stream()
                    .filter(img -> img.getIsPrimary() != null && img.getIsPrimary())
                    .findFirst();

            if (primaryImage.isPresent()) {
                return primaryImage.get().getUrl();
            } else if (!images.isEmpty() && images.get(0) != null && images.get(0).getUrl() != null) {
                return images.get(0).getUrl();
            } else {
                return DEFAULT_HOTEL_IMAGE;
            }
        } catch (Exception e) {
            logger.error("Error getting hotel primary image URL", e);
            return DEFAULT_HOTEL_IMAGE;
        }
    }

    public String getHotelPrimaryImageUrl(HotelResponse hotel) {
        if (hotel == null) {
            return DEFAULT_HOTEL_IMAGE;
        }

        try {
            // First check if imageUrl is already set
            if (hotel.getImageUrl() != null && !hotel.getImageUrl().isEmpty()) {
                return hotel.getImageUrl();
            }

            // Then check if primaryImageUrl is already set
            if (hotel.getPrimaryImageUrl() != null && !hotel.getPrimaryImageUrl().isEmpty()) {
                return hotel.getPrimaryImageUrl();
            }

            List<ImageResponse> images = hotel.getImages();
            if (images == null || images.isEmpty()) {
                return DEFAULT_HOTEL_IMAGE;
            }

            Optional<ImageResponse> primaryImage = images.stream()
                    .filter(img -> img.getIsPrimary() != null && img.getIsPrimary())
                    .findFirst();

            if (primaryImage.isPresent()) {
                return primaryImage.get().getUrl();
            } else if (!images.isEmpty() && images.get(0) != null && images.get(0).getUrl() != null) {
                return images.get(0).getUrl();
            } else {
                return DEFAULT_HOTEL_IMAGE;
            }
        } catch (Exception e) {
            logger.error("Error getting hotel primary image URL", e);
            return DEFAULT_HOTEL_IMAGE;
        }
    }

    public void setPrimaryImageUrl(HotelResponse hotel) {
        if (hotel == null) {
            return;
        }

        String imageUrl = getHotelPrimaryImageUrl(hotel);
        hotel.setImageUrl(imageUrl);
        hotel.setPrimaryImageUrl(imageUrl);
    }
}
