package com.hotelreservation.service;

import com.hotelreservation.dto.response.ImageResponse;
import com.hotelreservation.exception.BadRequestException;
import com.hotelreservation.exception.ResourceNotFoundException;
import com.hotelreservation.model.Image;
import com.hotelreservation.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private final ImageRepository imageRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.max-file-size:5MB}")
    private String maxFileSize;

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    /**
     * Get all images for a specific hotel
     *
     * @param hotelId The ID of the hotel
     * @return List of image responses
     */
    public List<ImageResponse> getHotelImages(Long hotelId) {
        log.info("Fetching images for hotel: {}", hotelId);
        List<ImageResponse> images = getEntityImages("HOTEL", hotelId);
        log.info("Found {} images for hotel {}", images.size(), hotelId);
        return images;
    }

    /**
     * Get all images for a specific room
     *
     * @param roomId The ID of the room
     * @return List of image responses
     */
    public List<ImageResponse> getRoomImages(Long roomId) {
        log.info("Fetching images for room: {}", roomId);
        List<ImageResponse> images = getEntityImages("ROOM", roomId);
        log.info("Found {} images for room {}", images.size(), roomId);
        return images;
    }

    @Transactional
    public List<ImageResponse> uploadHotelImages(Long hotelId, List<MultipartFile> files) {
        log.info("Uploading {} images for hotel: {}", files.size(), hotelId);

        return files.stream()
                .filter(file -> !file.isEmpty())
                .map(file -> uploadSingleImage(hotelId, "HOTEL", file))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ImageResponse> uploadRoomImages(Long roomId, List<MultipartFile> files) {
        log.info("Uploading {} images for room: {}", files.size(), roomId);

        return files.stream()
                .filter(file -> !file.isEmpty())
                .map(file -> uploadSingleImage(roomId, "ROOM", file))
                .collect(Collectors.toList());
    }

    private ImageResponse uploadSingleImage(Long entityId, String entityType, MultipartFile file) {
        try {
            // Validate file
            validateFile(file);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String filename = UUID.randomUUID().toString() + "." + extension;

            // Get the actual upload path
            Path actualUploadPath = getActualUploadPath();

            // Ensure directory exists
            if (!Files.exists(actualUploadPath)) {
                Files.createDirectories(actualUploadPath);
                log.info("Created upload directory: {}", actualUploadPath.toAbsolutePath());
            }

            // Save file to disk
            Path filePath = actualUploadPath.resolve(filename);
            log.info("Saving file to: {}", filePath.toAbsolutePath());

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Verify file was created
            if (Files.exists(filePath)) {
                log.info("File successfully saved: {}", filePath.toAbsolutePath());
            } else {
                log.error("File was not created: {}", filePath.toAbsolutePath());
                throw new RuntimeException("File was not saved to disk");
            }

            // Check if this is the first image (make it primary)
            List<Image> existingImages = imageRepository.findByEntityTypeAndEntityId(entityType, entityId);
            boolean isPrimary = existingImages.isEmpty();

            // Save image record to database
            String imageUrl = "/images/uploads/" + filename;
            log.info("Saving image URL to database: {}", imageUrl);

            Image image = Image.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .url(imageUrl)
                    .isPrimary(isPrimary)
                    .build();

            Image savedImage = imageRepository.save(image);
            log.info("Successfully uploaded image: {} for {} {} with URL: {}", filename, entityType, entityId, imageUrl);

            return mapToImageResponse(savedImage);
        } catch (IOException e) {
            log.error("Error uploading image for {} {}: {}", entityType, entityId, e.getMessage());
            throw new RuntimeException("Failed to upload image: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteImage(Long imageId) {
        log.info("Deleting image: {}", imageId);

        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        try {
            // Delete file from disk
            String filename = image.getUrl().substring(image.getUrl().lastIndexOf("/") + 1);
            Path actualUploadPath = getActualUploadPath();
            Path filePath = actualUploadPath.resolve(filename);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted file: {}", filePath.toAbsolutePath());
            } else {
                log.warn("File not found for deletion: {}", filePath.toAbsolutePath());
            }

            // Delete from database
            imageRepository.delete(image);

            // If this was the primary image, set another image as primary
            if (image.getIsPrimary()) {
                List<Image> remainingImages = imageRepository.findByEntityTypeAndEntityId(
                        image.getEntityType(), image.getEntityId());
                if (!remainingImages.isEmpty()) {
                    Image newPrimary = remainingImages.get(0);
                    newPrimary.setIsPrimary(true);
                    imageRepository.save(newPrimary);
                }
            }

            log.info("Successfully deleted image: {}", imageId);
        } catch (IOException e) {
            log.error("Error deleting image file: {}", e.getMessage());
            // Still delete from database even if file deletion fails
            imageRepository.delete(image);
        }
    }

    @Transactional
    public void setPrimaryImage(Long imageId) {
        log.info("Setting primary image: {}", imageId);

        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        // Remove primary flag from other images of the same entity
        List<Image> entityImages = imageRepository.findByEntityTypeAndEntityId(
                image.getEntityType(), image.getEntityId());

        for (Image entityImage : entityImages) {
            entityImage.setIsPrimary(false);
        }
        imageRepository.saveAll(entityImages);

        // Set this image as primary
        image.setIsPrimary(true);
        imageRepository.save(image);

        log.info("Successfully set image {} as primary", imageId);
    }

    public List<ImageResponse> getEntityImages(String entityType, Long entityId) {
        List<Image> images = imageRepository.findByEntityTypeAndEntityId(entityType, entityId);
        return images.stream()
                .map(this::mapToImageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get the actual upload path, handling both development and production environments
     */
    private Path getActualUploadPath() {
        // For development: use src/main/resources/static/images/uploads
        // For production: use classpath-relative path

        Path devPath = Paths.get("src/main/resources/static/images", uploadDir);

        // Check if we're in development environment
        if (Files.exists(Paths.get("src/main/resources"))) {
            log.debug("Using development upload path: {}", devPath.toAbsolutePath());
            return devPath;
        } else {
            // Production environment - use relative path
            Path prodPath = Paths.get("static/images", uploadDir);
            log.debug("Using production upload path: {}", prodPath.toAbsolutePath());
            return prodPath;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed size of " + maxFileSize);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new BadRequestException("Invalid filename");
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("File type not allowed. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BadRequestException("Invalid file extension");
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private ImageResponse mapToImageResponse(Image image) {
        return ImageResponse.builder()
                .id(image.getId())
                .url(image.getUrl())
                .isPrimary(image.getIsPrimary())
                .build();
    }
}
