package com.hotelreservation.controller.view;

import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.dto.response.ReviewResponse;
import com.hotelreservation.dto.response.RoomResponse;
import com.hotelreservation.dto.response.UserResponse;
import com.hotelreservation.model.BookingStatus;
import com.hotelreservation.repository.BookingRepository;
import com.hotelreservation.repository.HotelRepository;
import com.hotelreservation.repository.ReviewRepository;
import com.hotelreservation.repository.UserRepository;
import com.hotelreservation.service.AdminService;
import com.hotelreservation.service.BookingService;
import com.hotelreservation.service.BookingStatusUpdateService;
import com.hotelreservation.service.HotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final ReviewRepository reviewRepository;
    private final BookingService bookingService;
    private final BookingStatusUpdateService bookingStatusUpdateService;
    private final AdminService adminService;
    private final HotelService hotelService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        log.info("Accessing admin dashboard");
        try {
            // Get counts for dashboard
            long totalUsers = userRepository.count();
            long totalHotels = hotelRepository.count();
            long totalBookings = bookingRepository.count();
            long totalReviews = reviewRepository.count();

            // Get booking counts by status
            long pendingBookings = bookingService.countByStatus(BookingStatus.PENDING);
            long confirmedBookings = bookingService.countByStatus(BookingStatus.CONFIRMED);
            long completedBookings = bookingService.countByStatus(BookingStatus.COMPLETED);
            long cancelledBookings = bookingService.countByStatus(BookingStatus.CANCELLED);

            // Add to model
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("totalHotels", totalHotels);
            model.addAttribute("totalBookings", totalBookings);
            model.addAttribute("totalReviews", totalReviews);

            model.addAttribute("pendingBookings", pendingBookings);
            model.addAttribute("confirmedBookings", confirmedBookings);
            model.addAttribute("completedBookings", completedBookings);
            model.addAttribute("cancelledBookings", cancelledBookings);

            // Add page title
            model.addAttribute("pageTitle", "Admin Dashboard");

            return "admin/dashboard";
        } catch (Exception e) {
            log.error("Error loading admin dashboard: {}", e.getMessage(), e);
            model.addAttribute("error", "Error loading dashboard data: " + e.getMessage());
            return "admin/dashboard";
        }
    }

    @GetMapping("/hotels")
    public String hotels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String status,
            Model model) {
        log.info("Accessing admin hotels page - page: {}, size: {}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            Page<HotelResponse> hotelsPage = adminService.getAllHotelsForApproval(pageable);

            model.addAttribute("hotels", hotelsPage);
            model.addAttribute("pageTitle", "Hotel Management");

            log.info("Loaded {} hotels for admin page", hotelsPage.getTotalElements());
            return "admin/hotels";
        } catch (Exception e) {
            log.error("Error loading admin hotels page: {}", e.getMessage(), e);
            model.addAttribute("error", "Error loading hotels: " + e.getMessage());
            model.addAttribute("hotels", Page.empty());
            return "admin/hotels";
        }
    }

    @GetMapping("/rooms")
    public String rooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) String status,
            Model model) {
        log.info("Accessing admin rooms page - page: {}, size: {}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

            // Get all hotels for the dropdown
            try {
                Page<HotelResponse> hotelsPage = hotelService.getAllHotels(PageRequest.of(0, 100));
                model.addAttribute("hotels", hotelsPage.getContent());
            } catch (Exception e) {
                log.warn("Could not load hotels for dropdown: {}", e.getMessage());
                model.addAttribute("hotels", java.util.Collections.emptyList());
            }

            // Get rooms with pagination
            Page<RoomResponse> roomsPage = adminService.getAllRooms(pageable);
            model.addAttribute("rooms", roomsPage);
            model.addAttribute("pageTitle", "Room Management");

            log.info("Loaded {} rooms for admin page", roomsPage.getTotalElements());
            return "admin/rooms";
        } catch (Exception e) {
            log.error("Error loading admin rooms page: {}", e.getMessage(), e);
            model.addAttribute("error", "Error loading rooms: " + e.getMessage());
            model.addAttribute("rooms", Page.empty());
            model.addAttribute("hotels", java.util.Collections.emptyList());
            return "admin/rooms";
        }
    }

    @GetMapping("/users")
    public String users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            Model model) {
        log.info("Accessing admin users page - page: {}, size: {}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            Page<UserResponse> usersPage = adminService.getAllUsers(pageable);

            model.addAttribute("users", usersPage);
            model.addAttribute("pageTitle", "User Management");

            log.info("Loaded {} users for admin page", usersPage.getTotalElements());
            return "admin/users";
        } catch (Exception e) {
            log.error("Error loading admin users page: {}", e.getMessage(), e);
            model.addAttribute("error", "Error loading users: " + e.getMessage());
            model.addAttribute("users", Page.empty());
            return "admin/users";
        }
    }

    @GetMapping("/reviews")
    public String reviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String status,
            Model model) {
        log.info("Accessing admin reviews page - page: {}, size: {}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

            // Get all hotels for the dropdown
            try {
                Page<HotelResponse> hotelsPage = hotelService.getAllHotels(PageRequest.of(0, 100));
                model.addAttribute("hotels", hotelsPage.getContent());
            } catch (Exception e) {
                log.warn("Could not load hotels for dropdown: {}", e.getMessage());
                model.addAttribute("hotels", java.util.Collections.emptyList());
            }

            // Get reviews with pagination
            Page<ReviewResponse> reviewsPage = adminService.getAllReviews(pageable);
            model.addAttribute("reviews", reviewsPage);
            model.addAttribute("pageTitle", "Review Management");

            log.info("Loaded {} reviews for admin page", reviewsPage.getTotalElements());
            return "admin/reviews";
        } catch (Exception e) {
            log.error("Error loading admin reviews page: {}", e.getMessage(), e);
            model.addAttribute("error", "Error loading reviews: " + e.getMessage());
            model.addAttribute("reviews", Page.empty());
            model.addAttribute("hotels", java.util.Collections.emptyList());
            return "admin/reviews";
        }
    }

    @GetMapping("/system")
    public String system(Model model) {
        log.info("Accessing admin system page");
        model.addAttribute("pageTitle", "System Settings");
        return "admin/system";
    }
}
