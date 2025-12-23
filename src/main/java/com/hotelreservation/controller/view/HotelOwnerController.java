package com.hotelreservation.controller.view;

import com.hotelreservation.dto.request.HotelRequest;
import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.dto.response.ImageResponse;
import com.hotelreservation.dto.response.RoomResponse;
import com.hotelreservation.exception.ResourceNotFoundException;
import com.hotelreservation.exception.UnauthorizedException;
import com.hotelreservation.model.Hotel;
import com.hotelreservation.model.User;
import com.hotelreservation.repository.UserRepository;
import com.hotelreservation.service.HotelService;
import com.hotelreservation.service.ImageService;
import com.hotelreservation.service.RoomService;
import com.hotelreservation.service.UserService;
import com.hotelreservation.util.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/hotel-owner")
@PreAuthorize("hasRole('HOTEL_OWNER')")
@RequiredArgsConstructor
@Slf4j
public class HotelOwnerController {

    private final HotelService hotelService;
    private final RoomService roomService;
    private final UserService userService;
    private final ImageService imageService;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        String email = authentication.getName();
        User currentUser = userService.findByEmail(email);

        List<Hotel> ownedHotels = hotelService.findHotelsByOwnerId(currentUser.getId());

        model.addAttribute("hotels", ownedHotels);
        model.addAttribute("hotelCount", ownedHotels.size());

        return Constants.VIEW_OWNER_DASHBOARD;
    }

    @GetMapping("/hotels")
    public String listHotels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException(Constants.MSG_ERR_USER_NOT_FOUND));

            Page<HotelResponse> hotelsPage = hotelService.getHotelsByOwner(user.getId(), PageRequest.of(page, size));

            model.addAttribute("hotels", hotelsPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", hotelsPage.getTotalPages());
            model.addAttribute("totalHotels", hotelsPage.getTotalElements());

            return Constants.VIEW_OWNER_HOTELS;
        } catch (Exception e) {
            log.error("Error listing owner hotels", e);
            model.addAttribute(Constants.ATTR_ERROR_MSG, "Error loading hotels");
            return Constants.VIEW_OWNER_HOTELS;
        }
    }

    @GetMapping("/hotels/new")
    public String newHotel(Model model) {
        model.addAttribute("hotelRequest", new HotelRequest());
        return Constants.VIEW_OWNER_HOTEL_FORM;
    }

    @PostMapping("/hotels/new")
    public String createHotel(
            @Valid @ModelAttribute HotelRequest hotelRequest,
            BindingResult result,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return Constants.VIEW_OWNER_HOTEL_FORM;
        }

        try {
            HotelResponse hotel = hotelService.createHotel(hotelRequest);

            // Upload images if provided
            if (images != null && !images.isEmpty()) {
                imageService.uploadHotelImages(hotel.getId(), images);
            }

            redirectAttributes.addFlashAttribute(Constants.ATTR_SUCCESS_MSG, "Hotel created successfully!");
            return Constants.REDIRECT_OWNER_HOTELS + "/" + hotel.getId();
        } catch (Exception e) {
            log.error("Error creating hotel", e);
            redirectAttributes.addFlashAttribute(Constants.ATTR_ERROR_MSG, "Error creating hotel: " + e.getMessage());
            return Constants.REDIRECT_OWNER_HOTELS + "/new";
        }
    }

    @GetMapping("/hotels/{id}")
    public String viewHotel(@PathVariable Long id, Model model) {
        try {
            HotelResponse hotel = hotelService.getHotelById(id);

            // Verify ownership
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (hotel.getOwner() == null || !hotel.getOwner().getId().equals(user.getId())) {
                throw new UnauthorizedException("You don't have permission to view this hotel");
            }

            // Get hotel rooms
            Page<RoomResponse> roomsPage = roomService.getRoomsByHotel(id, PageRequest.of(0, 20));

            // Get hotel images
            List<ImageResponse> hotelImages = imageService.getEntityImages("HOTEL", id);

            model.addAttribute("hotel", hotel);
            model.addAttribute("rooms", roomsPage.getContent());
            model.addAttribute("hotelImages", hotelImages);

            return Constants.VIEW_OWNER_HOTEL_DETAIL;
        } catch (Exception e) {
            log.error("Error viewing hotel: {}", id, e);
            model.addAttribute(Constants.ATTR_ERROR_MSG, "Error loading hotel details");
            return Constants.VIEW_OWNER_HOTELS;
        }
    }

    @GetMapping("/hotels/{id}/edit")
    public String editHotel(@PathVariable Long id, Model model) {
        try {
            HotelResponse hotel = hotelService.getHotelById(id);

            // Verify ownership
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (hotel.getOwner() == null || !hotel.getOwner().getId().equals(user.getId())) {
                throw new UnauthorizedException("You don't have permission to edit this hotel");
            }

            // Convert to request object
            HotelRequest hotelRequest = HotelRequest.builder()
                    .name(hotel.getName())
                    .description(hotel.getDescription())
                    .address(hotel.getAddress())
                    .city(hotel.getCity())
                    .country(hotel.getCountry())
                    .postalCode(hotel.getPostalCode())
                    .latitude(hotel.getLatitude())
                    .longitude(hotel.getLongitude())
                    .starRating(hotel.getStarRating())
                    .build();

            model.addAttribute("hotel", hotel);
            model.addAttribute("hotelRequest", hotelRequest);

            return Constants.VIEW_OWNER_HOTEL_EDIT;
        } catch (Exception e) {
            log.error("Error loading hotel for edit: {}", id, e);
            model.addAttribute(Constants.ATTR_ERROR_MSG, "Error loading hotel");
            return Constants.REDIRECT_OWNER_HOTELS;
        }
    }

    @PostMapping("/hotels/{id}/edit")
    public String updateHotel(
            @PathVariable Long id,
            @Valid @ModelAttribute HotelRequest hotelRequest,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return Constants.VIEW_OWNER_HOTEL_EDIT;
        }

        try {
            hotelService.updateHotel(id, hotelRequest);
            redirectAttributes.addFlashAttribute(Constants.ATTR_SUCCESS_MSG, "Hotel updated successfully!");
            return Constants.REDIRECT_OWNER_HOTELS + "/" + id;
        } catch (Exception e) {
            log.error("Error updating hotel: {}", id, e);
            redirectAttributes.addFlashAttribute(Constants.ATTR_ERROR_MSG, "Error updating hotel: " + e.getMessage());
            return Constants.REDIRECT_OWNER_HOTELS + "/" + id + "/edit";
        }
    }

    @PostMapping("/hotels/{id}/images/upload")
    public String uploadImages(
            @PathVariable Long id,
            @RequestParam("images") List<MultipartFile> images,
            RedirectAttributes redirectAttributes) {

        try {
            // Verify ownership before allowing upload
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            HotelResponse hotel = hotelService.getHotelById(id);
            if (hotel.getOwner() == null || !hotel.getOwner().getId().equals(user.getId())) {
                throw new UnauthorizedException("You don't have permission to upload images for this hotel");
            }

            List<ImageResponse> uploadedImages = imageService.uploadHotelImages(id, images);
            redirectAttributes.addFlashAttribute(Constants.ATTR_SUCCESS_MSG,
                    "Successfully uploaded " + uploadedImages.size() + " image(s)!");
        } catch (Exception e) {
            log.error("Error uploading images for hotel: {}", id, e);
            redirectAttributes.addFlashAttribute(Constants.ATTR_ERROR_MSG, "Error uploading images: " + e.getMessage());
        }

        return Constants.REDIRECT_OWNER_HOTELS + "/" + id;
    }

    @PostMapping("/hotels/{hotelId}/images/{imageId}/delete")
    public String deleteImage(
            @PathVariable Long hotelId,
            @PathVariable Long imageId,
            RedirectAttributes redirectAttributes) {

        try {
            // Verify ownership before allowing deletion
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            HotelResponse hotel = hotelService.getHotelById(hotelId);
            if (hotel.getOwner() == null || !hotel.getOwner().getId().equals(user.getId())) {
                throw new UnauthorizedException("You don't have permission to delete images for this hotel");
            }

            imageService.deleteImage(imageId);
            redirectAttributes.addFlashAttribute(Constants.ATTR_SUCCESS_MSG, "Image deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting image: {}", imageId, e);
            redirectAttributes.addFlashAttribute(Constants.ATTR_ERROR_MSG, "Error deleting image: " + e.getMessage());
        }

        return Constants.REDIRECT_OWNER_HOTELS + "/" + hotelId;
    }

    @PostMapping("/hotels/{hotelId}/images/{imageId}/set-primary")
    public String setPrimaryImage(
            @PathVariable Long hotelId,
            @PathVariable Long imageId,
            RedirectAttributes redirectAttributes) {

        try {
            // Verify ownership before allowing primary image change
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            HotelResponse hotel = hotelService.getHotelById(hotelId);
            if (hotel.getOwner() == null || !hotel.getOwner().getId().equals(user.getId())) {
                throw new UnauthorizedException("You don't have permission to modify images for this hotel");
            }

            imageService.setPrimaryImage(imageId);
            redirectAttributes.addFlashAttribute(Constants.ATTR_SUCCESS_MSG, "Primary image updated successfully!");
        } catch (Exception e) {
            log.error("Error setting primary image: {}", imageId, e);
            redirectAttributes.addFlashAttribute(Constants.ATTR_ERROR_MSG,
                    "Error setting primary image: " + e.getMessage());
        }

        return Constants.REDIRECT_OWNER_HOTELS + "/" + hotelId;
    }

    @GetMapping("/hotels/{hotelId}/rooms/{roomId}")
    public String viewRoom(@PathVariable Long hotelId, @PathVariable Long roomId, Model model) {
        try {
            // Verify hotel ownership
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            HotelResponse hotel = hotelService.getHotelById(hotelId);
            if (hotel.getOwner() == null || !hotel.getOwner().getId().equals(user.getId())) {
                throw new UnauthorizedException("You don't have permission to view this room");
            }

            RoomResponse room = roomService.getRoomById(roomId);
            if (!room.getHotelId().equals(hotelId)) {
                throw new UnauthorizedException("Room does not belong to this hotel");
            }

            // Get room images
            List<ImageResponse> roomImages = imageService.getRoomImages(roomId);

            model.addAttribute("hotel", hotel);
            model.addAttribute("room", room);
            model.addAttribute("roomImages", roomImages);

            return Constants.VIEW_OWNER_ROOM_DETAIL;
        } catch (Exception e) {
            log.error("Error viewing room: {}", roomId, e);
            model.addAttribute(Constants.ATTR_ERROR_MSG, "Error loading room details");
            return Constants.REDIRECT_OWNER_HOTELS + "/" + hotelId;
        }
    }

    @PostMapping("/hotels/{hotelId}/rooms/{roomId}/images/upload")
    public String uploadRoomImages(
            @PathVariable Long hotelId,
            @PathVariable Long roomId,
            @RequestParam("images") List<MultipartFile> images,
            RedirectAttributes redirectAttributes) {

        try {
            // Verify ownership
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            HotelResponse hotel = hotelService.getHotelById(hotelId);
            if (hotel.getOwner() == null || !hotel.getOwner().getId().equals(user.getId())) {
                throw new UnauthorizedException("You don't have permission to upload images for this room");
            }

            RoomResponse room = roomService.getRoomById(roomId);
            if (!room.getHotelId().equals(hotelId)) {
                throw new UnauthorizedException("Room does not belong to this hotel");
            }

            List<ImageResponse> uploadedImages = imageService.uploadRoomImages(roomId, images);
            redirectAttributes.addFlashAttribute(Constants.ATTR_SUCCESS_MSG,
                    "Successfully uploaded " + uploadedImages.size() + " image(s)!");
        } catch (Exception e) {
            log.error("Error uploading images for room: {}", roomId, e);
            redirectAttributes.addFlashAttribute(Constants.ATTR_ERROR_MSG, "Error uploading images: " + e.getMessage());
        }

        return Constants.REDIRECT_OWNER_HOTELS + "/" + hotelId + "/rooms/" + roomId;
    }

    @PostMapping("/hotels/{hotelId}/rooms/{roomId}/images/{imageId}/delete")
    public String deleteRoomImage(
            @PathVariable Long hotelId,
            @PathVariable Long roomId,
            @PathVariable Long imageId,
            RedirectAttributes redirectAttributes) {

        try {
            // Verify ownership
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            HotelResponse hotel = hotelService.getHotelById(hotelId);
            if (hotel.getOwner() == null || !hotel.getOwner().getId().equals(user.getId())) {
                throw new UnauthorizedException("You don't have permission to delete images for this room");
            }

            imageService.deleteImage(imageId);
            redirectAttributes.addFlashAttribute(Constants.ATTR_SUCCESS_MSG, "Image deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting room image: {}", imageId, e);
            redirectAttributes.addFlashAttribute(Constants.ATTR_ERROR_MSG, "Error deleting image: " + e.getMessage());
        }

        return Constants.REDIRECT_OWNER_HOTELS + "/" + hotelId + "/rooms/" + roomId;
    }

    @PostMapping("/hotels/{hotelId}/rooms/{roomId}/images/{imageId}/set-primary")
    public String setPrimaryRoomImage(
            @PathVariable Long hotelId,
            @PathVariable Long roomId,
            @PathVariable Long imageId,
            RedirectAttributes redirectAttributes) {

        try {
            // Verify ownership
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            HotelResponse hotel = hotelService.getHotelById(hotelId);
            if (hotel.getOwner() == null || !hotel.getOwner().getId().equals(user.getId())) {
                throw new UnauthorizedException("You don't have permission to modify images for this room");
            }

            imageService.setPrimaryImage(imageId);
            redirectAttributes.addFlashAttribute(Constants.ATTR_SUCCESS_MSG, "Primary image updated successfully!");
        } catch (Exception e) {
            log.error("Error setting primary room image: {}", imageId, e);
            redirectAttributes.addFlashAttribute(Constants.ATTR_ERROR_MSG,
                    "Error setting primary image: " + e.getMessage());
        }

        return Constants.REDIRECT_OWNER_HOTELS + "/" + hotelId + "/rooms/" + roomId;
    }
}
