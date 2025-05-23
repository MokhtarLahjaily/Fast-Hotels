package com.hotelreservation.controller.view;

import com.hotelreservation.dto.request.HotelRequest;
import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.dto.response.RoomResponse;
import com.hotelreservation.dto.response.UserResponse;
import com.hotelreservation.exception.ResourceNotFoundException;
import com.hotelreservation.exception.UnauthorizedException;
import com.hotelreservation.model.Hotel;
import com.hotelreservation.model.User;
import com.hotelreservation.repository.UserRepository;
import com.hotelreservation.service.HotelService;
import com.hotelreservation.service.ImageService;
import com.hotelreservation.service.RoomService;
import com.hotelreservation.service.UserService;
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

        return "hotel-owner/dashboard";
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
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            Page<HotelResponse> hotelsPage = hotelService.getHotelsByOwner(user.getId(), PageRequest.of(page, size));

            model.addAttribute("hotels", hotelsPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", hotelsPage.getTotalPages());
            model.addAttribute("totalHotels", hotelsPage.getTotalElements());

            return "hotel-owner/hotels";
        } catch (Exception e) {
            log.error("Error listing owner hotels", e);
            model.addAttribute("errorMessage", "Error loading hotels");
            return "hotel-owner/hotels";
        }
    }

    @GetMapping("/hotels/new")
    public String newHotel(Model model) {
        model.addAttribute("hotelRequest", new HotelRequest());
        return "hotel-owner/hotel-form";
    }

    @PostMapping("/hotels/new")
    public String createHotel(
            @Valid @ModelAttribute HotelRequest hotelRequest,
            BindingResult result,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "hotel-owner/hotel-form";
        }

        try {
            HotelResponse hotel = hotelService.createHotel(hotelRequest);

            // Upload images if provided
            if (images != null && !images.isEmpty()) {
                imageService.uploadHotelImages(hotel.getId(), images);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Hotel created successfully!");
            return "redirect:/hotel-owner/hotels/" + hotel.getId();
        } catch (Exception e) {
            log.error("Error creating hotel", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating hotel: " + e.getMessage());
            return "redirect:/hotel-owner/hotels/new";
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

            if (!hotel.getOwner().getId().equals(user.getId())) {
                throw new UnauthorizedException("You don't have permission to view this hotel");
            }

            // Get hotel rooms
            Page<RoomResponse> roomsPage = roomService.getRoomsByHotel(id, PageRequest.of(0, 20));

            model.addAttribute("hotel", hotel);
            model.addAttribute("rooms", roomsPage.getContent());

            return "hotel-owner/hotel-detail";
        } catch (Exception e) {
            log.error("Error viewing hotel: {}", id, e);
            model.addAttribute("errorMessage", "Error loading hotel details");
            return "hotel-owner/hotels";
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

            if (!hotel.getOwner().getId().equals(user.getId())) {
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

            return "hotel-owner/hotel-edit";
        } catch (Exception e) {
            log.error("Error loading hotel for edit: {}", id, e);
            model.addAttribute("errorMessage", "Error loading hotel");
            return "redirect:/hotel-owner/hotels";
        }
    }

    @PostMapping("/hotels/{id}/edit")
    public String updateHotel(
            @PathVariable Long id,
            @Valid @ModelAttribute HotelRequest hotelRequest,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "hotel-owner/hotel-edit";
        }

        try {
            hotelService.updateHotel(id, hotelRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Hotel updated successfully!");
            return "redirect:/hotel-owner/hotels/" + id;
        } catch (Exception e) {
            log.error("Error updating hotel: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating hotel: " + e.getMessage());
            return "redirect:/hotel-owner/hotels/" + id + "/edit";
        }
    }

    @PostMapping("/hotels/{id}/images/upload")
    public String uploadImages(
            @PathVariable Long id,
            @RequestParam("images") List<MultipartFile> images,
            RedirectAttributes redirectAttributes) {

        try {
            imageService.uploadHotelImages(id, images);
            redirectAttributes.addFlashAttribute("successMessage", "Images uploaded successfully!");
        } catch (Exception e) {
            log.error("Error uploading images for hotel: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error uploading images: " + e.getMessage());
        }

        return "redirect:/hotel-owner/hotels/" + id;
    }

    @PostMapping("/hotels/{hotelId}/images/{imageId}/delete")
    public String deleteImage(
            @PathVariable Long hotelId,
            @PathVariable Long imageId,
            RedirectAttributes redirectAttributes) {

        try {
            imageService.deleteImage(imageId);
            redirectAttributes.addFlashAttribute("successMessage", "Image deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting image: {}", imageId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting image: " + e.getMessage());
        }

        return "redirect:/hotel-owner/hotels/" + hotelId;
    }

    @PostMapping("/hotels/{hotelId}/images/{imageId}/set-primary")
    public String setPrimaryImage(
            @PathVariable Long hotelId,
            @PathVariable Long imageId,
            RedirectAttributes redirectAttributes) {

        try {
            imageService.setPrimaryImage(imageId);
            redirectAttributes.addFlashAttribute("successMessage", "Primary image updated successfully!");
        } catch (Exception e) {
            log.error("Error setting primary image: {}", imageId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error setting primary image: " + e.getMessage());
        }

        return "redirect:/hotel-owner/hotels/" + hotelId;
    }
}
