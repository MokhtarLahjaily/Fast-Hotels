package com.hotelreservation.controller.view;

import com.hotelreservation.dto.request.PasswordChangeRequest;
import com.hotelreservation.dto.request.ProfileUpdateRequest;
import com.hotelreservation.dto.response.BookingResponse;
import com.hotelreservation.dto.response.UserResponse;
import com.hotelreservation.exception.BadRequestException;
import com.hotelreservation.exception.UnauthorizedException;
import com.hotelreservation.exception.UserAlreadyExistsException;
import com.hotelreservation.model.User;
import com.hotelreservation.repository.UserRepository;
import com.hotelreservation.service.BookingService;
import com.hotelreservation.service.UserService;
import com.hotelreservation.util.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final UserService userService;
    private final BookingService bookingService;
    private final UserRepository userRepository;

    @GetMapping
    public String showProfile(Model model) {
        log.info("Showing profile page");

        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException(Constants.MSG_ERR_USER_NOT_FOUND));

            // Create UserResponse manually
            UserResponse userResponse = new UserResponse();
            userResponse.setId(user.getId());
            userResponse.setEmail(user.getEmail());
            userResponse.setFirstName(user.getFirstName());
            userResponse.setLastName(user.getLastName());
            userResponse.setPhone(user.getPhone());
            userResponse.setRole(user.getRole().toString());

            // Add user data to model
            model.addAttribute("user", userResponse);

            // Create and add profile update request
            if (!model.containsAttribute("profileUpdateRequest")) {
                ProfileUpdateRequest profileUpdateRequest = new ProfileUpdateRequest();
                profileUpdateRequest.setFirstName(user.getFirstName());
                profileUpdateRequest.setLastName(user.getLastName());
                profileUpdateRequest.setEmail(user.getEmail());
                profileUpdateRequest.setPhone(user.getPhone());

                model.addAttribute("profileUpdateRequest", profileUpdateRequest);
            }

            // Create and add password change request
            if (!model.containsAttribute("passwordChangeRequest")) {
                model.addAttribute("passwordChangeRequest", new PasswordChangeRequest());
            }

            // Add recent bookings with proper error handling
            try {
                List<BookingResponse> recentBookings = bookingService.getRecentBookingsForCurrentUser(3);

                // Validate each booking to ensure it has the required properties
                for (BookingResponse booking : recentBookings) {
                    if (booking.getHotelName() == null && booking.getHotel() != null) {
                        booking.setHotelName(booking.getHotel().getName());
                    } else if (booking.getHotelName() == null) {
                        booking.setHotelName(Constants.DEFAULT_NA);
                    }

                    if (booking.getRoomType() == null && booking.getRoom() != null) {
                        booking.setRoomType(booking.getRoom().getType());
                    } else if (booking.getRoomType() == null) {
                        booking.setRoomType(Constants.DEFAULT_NA);
                    }
                }

                model.addAttribute("recentBookings", recentBookings);
                log.debug("Added {} recent bookings to model", recentBookings.size());
            } catch (Exception e) {
                log.error("Error fetching recent bookings: {}", e.getMessage(), e);
                model.addAttribute("recentBookings", Collections.emptyList());
                model.addAttribute("bookingError", "Unable to load recent bookings. Please try again later.");
            }

            return Constants.VIEW_PROFILE;
        } catch (Exception e) {
            log.error("Error showing profile page: {}", e.getMessage(), e);
            return Constants.REDIRECT_PREFIX + "/error";
        }
    }

    @PostMapping("/update")
    public String updateProfile(
            @Valid @ModelAttribute("profileUpdateRequest") ProfileUpdateRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        log.info("Processing profile update for user");

        if (result.hasErrors()) {
            log.warn("Profile update validation failed: {}", result.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.profileUpdateRequest",
                    result);
            redirectAttributes.addFlashAttribute("profileUpdateRequest", request);
            return Constants.REDIRECT_PROFILE;
        }

        try {
            UserResponse updatedUser = userService.updateProfile(request);
            log.info("Profile updated successfully for user: {}", updatedUser.getEmail());
            redirectAttributes.addFlashAttribute(Constants.ATTR_SUCCESS_MSG, Constants.MSG_PROFILE_UPDATED);
        } catch (UserAlreadyExistsException e) {
            log.warn("Profile update failed: {}", e.getMessage());
            result.rejectValue("email", "error.email", "Email already in use");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.profileUpdateRequest",
                    result);
            redirectAttributes.addFlashAttribute("profileUpdateRequest", request);
            return Constants.REDIRECT_PROFILE;
        } catch (UnauthorizedException e) {
            log.warn("Password change failed: {}", e.getMessage());
            result.rejectValue("currentPassword", "error.currentPassword", "Current password is incorrect");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.profileUpdateRequest",
                    result);
            redirectAttributes.addFlashAttribute("profileUpdateRequest", request);
            return Constants.REDIRECT_PROFILE;
        } catch (BadRequestException e) {
            log.warn("Password change failed: {}", e.getMessage());
            result.rejectValue("confirmPassword", "error.confirmPassword", "New passwords do not match");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.profileUpdateRequest",
                    result);
            redirectAttributes.addFlashAttribute("profileUpdateRequest", request);
            return Constants.REDIRECT_PROFILE;
        } catch (Exception e) {
            log.error("Error updating profile", e);
            redirectAttributes.addFlashAttribute(Constants.ATTR_ERROR_MSG,
                    "An error occurred while updating your profile");
            redirectAttributes.addFlashAttribute("profileUpdateRequest", request);
            return Constants.REDIRECT_PROFILE;
        }

        return Constants.REDIRECT_PROFILE;
    }

    @PostMapping("/change-password")
    public String changePassword(
            @Valid @ModelAttribute("passwordChangeRequest") PasswordChangeRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        log.info("Processing password change");

        if (result.hasErrors()) {
            log.warn("Password change validation failed: {}", result.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.passwordChangeRequest",
                    result);
            redirectAttributes.addFlashAttribute("passwordChangeRequest", request);
            redirectAttributes.addFlashAttribute("passwordTab", true);
            return Constants.REDIRECT_PROFILE;
        }

        try {
            userService.changePassword(request);
            log.info("Password changed successfully");
            redirectAttributes.addFlashAttribute(Constants.ATTR_SUCCESS_MSG, "Password changed successfully");
        } catch (UnauthorizedException e) {
            log.warn("Password change failed: {}", e.getMessage());
            result.rejectValue("currentPassword", "error.currentPassword", "Current password is incorrect");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.passwordChangeRequest",
                    result);
            redirectAttributes.addFlashAttribute("passwordChangeRequest", request);
            redirectAttributes.addFlashAttribute("passwordTab", true);
            return Constants.REDIRECT_PROFILE;
        } catch (BadRequestException e) {
            log.warn("Password change failed: {}", e.getMessage());
            result.rejectValue("confirmPassword", "error.confirmPassword", "New passwords do not match");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.passwordChangeRequest",
                    result);
            redirectAttributes.addFlashAttribute("passwordChangeRequest", request);
            redirectAttributes.addFlashAttribute("passwordTab", true);
            return Constants.REDIRECT_PROFILE;
        } catch (Exception e) {
            log.error("Error changing password", e);
            redirectAttributes.addFlashAttribute(Constants.ATTR_ERROR_MSG,
                    "An error occurred while changing your password");
            redirectAttributes.addFlashAttribute("passwordChangeRequest", request);
            redirectAttributes.addFlashAttribute("passwordTab", true);
            return Constants.REDIRECT_PROFILE;
        }

        return Constants.REDIRECT_PROFILE;
    }
}
